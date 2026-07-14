package com.avides.springboot.springtainer.common.cleanup;

import static com.avides.springboot.springtainer.common.Labels.SPRINGTAINER_ISSUER;
import static com.avides.springboot.springtainer.common.Labels.SPRINGTAINER_STARTED;
import static java.util.Comparator.comparingLong;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.avides.springboot.springtainer.common.util.DockerClients;
import com.avides.springboot.springtainer.common.util.IssuerUtil;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.Container;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Configuration
@ConditionalOnProperty(name = "embedded.container.cleanup.enabled", matchIfMissing = true)
@EnableConfigurationProperties(ContainerCleanupProperties.class)
@Slf4j
public class EmbeddedContainerCleanupAutoConfiguration
{
    // Spring's test-context cache keeps many contexts (and thus many EmbeddedContainerCleanup beans, one per cached context) alive concurrently, so a
    // per-bean scheduler/shutdown-hook would run redundantly once per cached context. A single JVM-wide instance of each avoids that.
    private static final AtomicBoolean SCHEDULER_STARTED = new AtomicBoolean();

    private static final AtomicBoolean SHUTDOWN_HOOK_REGISTERED = new AtomicBoolean();

    @ConditionalOnMissingBean(EmbeddedContainerCleanup.class)
    @Bean
    public EmbeddedContainerCleanup embeddedContainerCleanup(ContainerCleanupProperties properties)
    {
        return new EmbeddedContainerCleanup(properties);
    }

    @RequiredArgsConstructor
    public static class EmbeddedContainerCleanup
    {
        // A large test suite creates many distinct Spring test-context configurations (each getting its own EmbeddedContainerCleanup bean), often within
        // milliseconds of each other. Debouncing collapses those into a single Docker round-trip instead of re-scanning the whole host every time, while
        // still re-checking every DEBOUNCE_MILLIS as the suite progresses - unlike a one-shot guard, this keeps maxConcurrentPerIssuer enforcement working
        // throughout a long run as more containers accumulate over time, not just at the very first context's creation.
        private static final long STALE_CHECK_DEBOUNCE_MILLIS = 5_000;

        private static final AtomicLong LAST_STALE_CHECK_MILLIS = new AtomicLong();

        public EmbeddedContainerCleanup(ContainerCleanupProperties properties)
        {
            log.info("{} stale containers removed", Integer.valueOf(removeStaleContainersIfDue(properties)));
            startScheduledCheckIfNeeded(properties);
            registerShutdownHookIfNeeded();
        }

        private static int removeStaleContainersIfDue(ContainerCleanupProperties properties)
        {
            long now = System.currentTimeMillis();
            long last = LAST_STALE_CHECK_MILLIS.get();

            if (now - last < STALE_CHECK_DEBOUNCE_MILLIS || !LAST_STALE_CHECK_MILLIS.compareAndSet(last, now))
            {
                return 0;
            }

            return removeStaleContainers(properties);
        }

        /**
         * Resets the stale-check debounce so each test can independently exercise {@link #removeStaleContainers}, instead of only the first test in this
         * class actually reaching it.
         */
        static void resetStaleCheckDebounceForTesting() // NOSONAR - package-private test hook, deliberately not part of the public API
        {
            LAST_STALE_CHECK_MILLIS.set(0);
        }

        private static void startScheduledCheckIfNeeded(ContainerCleanupProperties properties)
        {
            if (properties.getCheckIntervalSeconds() > 0 && SCHEDULER_STARTED.compareAndSet(false, true))
            {
                ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> newDaemonThread(runnable, "springtainer-cleanup-scheduler"));

                long intervalSeconds = properties.getCheckIntervalSeconds();
                scheduler.scheduleWithFixedDelay(() -> runScheduledCheck(properties), intervalSeconds, intervalSeconds, TimeUnit.SECONDS);
            }
        }

        private static void runScheduledCheck(ContainerCleanupProperties properties)
        {
            try
            {
                removeStaleContainers(properties);
            }
            catch (Exception e) // NOSONAR - a failed periodic check must never bring down the scheduler thread
            {
                log.warn("Scheduled stale-container check failed", e);
            }
        }

        /**
         * Registers a plain JVM shutdown hook (independent of any {@link org.springframework.context.ApplicationContext}) that force-removes every remaining
         * container for the current issuer once this JVM actually exits.
         * <p>
         * The "normal" cleanup path relies on Spring's test-context cache eventually closing every cached context, but a build tool's forked JVM doesn't
         * reliably give each cached context's own shutdown hook enough time to complete once it starts exiting several at once. This hook is a
         * Spring-independent backstop that guarantees no container is left behind regardless.
         */
        private static void registerShutdownHookIfNeeded()
        {
            if (SHUTDOWN_HOOK_REGISTERED.compareAndSet(false, true))
            {
                Runtime.getRuntime().addShutdownHook(newDaemonThread(EmbeddedContainerCleanup::removeAllContainersForCurrentIssuer, "springtainer-cleanup-shutdown-hook"));
            }
        }

        private static Thread newDaemonThread(Runnable runnable, String name)
        {
            Thread thread = new Thread(runnable, name);
            thread.setDaemon(true);
            return thread;
        }

        private static void removeAllContainersForCurrentIssuer()
        {
            try
            {
                String currentIssuer = IssuerUtil.getIssuer();
                DockerClient dockerClient = DockerClients.shared();

                for (Container container : dockerClient.listContainersCmd().withLabelFilter(Map.of(SPRINGTAINER_ISSUER, currentIssuer)).exec())
                {
                    removeContainer(dockerClient, container);
                }
            }
            catch (Exception e) // NOSONAR - a failing shutdown hook must never prevent JVM shutdown from completing
            {
                log.warn("Failed to remove containers for the current issuer on JVM shutdown", e);
            }
        }

        @SneakyThrows
        private static int removeStaleContainers(ContainerCleanupProperties properties)
        {
            String currentIssuer = IssuerUtil.getIssuer();
            List<Container> issuerContainers = new ArrayList<>();
            List<Container> staleContainers = new ArrayList<>();

            DockerClient dockerClient = DockerClients.shared();

            for (Container container : dockerClient.listContainersCmd().withLabelFilter(List.of(SPRINGTAINER_STARTED)).exec())
            {
                long millis = Long.parseLong(container.getLabels().get(SPRINGTAINER_STARTED));
                LocalDateTime started = LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault());
                LocalDateTime staleSince = LocalDateTime.now().minusMinutes(properties.getAfterMinutes());

                if (started.isBefore(staleSince))
                {
                    staleContainers.add(container);
                }
                else if (currentIssuer.equals(container.getLabels().get(SPRINGTAINER_ISSUER)))
                {
                    issuerContainers.add(container);
                }
            }

            int excess = issuerContainers.size() - properties.getMaxConcurrentPerIssuer();

            if (excess > 0)
            {
                // Only the oldest excess containers are removed, not all of them: a container started moments ago is very likely still in active use by
                // the test that just created it, whereas the oldest ones are the most likely to be sitting idle in Spring's context cache.
                List<Container> oldestExcessContainers = issuerContainers.stream()
                        .sorted(comparingLong(container -> Long.parseLong(container.getLabels().get(SPRINGTAINER_STARTED))))
                        .limit(excess)
                        .toList();

                staleContainers.addAll(oldestExcessContainers);
                log.warn("Too many concurrent containers ({}) for issuer \"{}\", removing the {} oldest", Integer
                        .valueOf(issuerContainers.size()), currentIssuer, Integer.valueOf(excess));
            }

            for (Container staleContainer : staleContainers)
            {
                removeContainer(dockerClient, staleContainer);
            }

            return staleContainers.size();
        }

        private static void removeContainer(DockerClient dockerClient, Container staleContainer)
        {
            try
            {
                dockerClient.removeContainerCmd(staleContainer.getId()).withForce(Boolean.TRUE).withRemoveVolumes(Boolean.TRUE).exec();
                log.warn("Stale container removed ({})", staleContainer.labels);
            }
            catch (NotFoundException e)
            {
                // already removed concurrently (e.g. by the container's own shutdown hook, or a parallel check) - nothing to do
            }
        }
    }
}
