package com.avides.springboot.springtainer.common.cleanup;

import static com.avides.springboot.springtainer.common.Labels.SPRINGTAINER_ISSUER;
import static com.avides.springboot.springtainer.common.Labels.SPRINGTAINER_STARTED;
import static java.util.Comparator.comparingLong;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

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
        public EmbeddedContainerCleanup(ContainerCleanupProperties properties)
        {
            log.info("{} stale containers removed", Integer.valueOf(removeStaleContainers(properties)));
            startScheduledCheckIfNeeded(properties);
            registerShutdownHookIfNeeded();
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
         * The "normal" cleanup path - {@code AbstractBuildingEmbeddedContainer} stopping its own container as a low-phase {@link org.springframework.context.SmartLifecycle}
         * bean during context close - relies on Spring's test-context cache eventually closing every cached context. In practice that often never happens for a
         * plain test run (no {@code @DirtiesContext}, well under the context cache's default eviction size), and each cached context's own {@code
         * SpringApplication}-registered JVM shutdown hook is not reliably given enough time to run to completion once a build tool's forked JVM starts
         * exiting many of them at once. Observed in practice: containers from several different cached contexts still running well after `mvn verify`
         * finished and the forked JVM had already exited. This hook is a direct, Spring-independent guarantee that this JVM does not leave its own
         * containers behind, regardless of how many contexts got cached or whether their individual shutdown hooks completed in time.
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

                try (DockerClient dockerClient = DockerClients.build())
                {
                    for (Container container : dockerClient.listContainersCmd().exec())
                    {
                        if (currentIssuer.equals(container.getLabels().get(SPRINGTAINER_ISSUER)))
                        {
                            removeContainer(dockerClient, container);
                        }
                    }
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

            try (DockerClient dockerClient = DockerClients.build())
            {
                for (Container container : dockerClient.listContainersCmd().exec())
                {
                    if (container.getLabels().containsKey(SPRINGTAINER_STARTED))
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
