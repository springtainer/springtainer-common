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
    // per-bean scheduler would run the same check redundantly once per cached context. A single JVM-wide scheduler avoids that.
    private static final AtomicBoolean SCHEDULER_STARTED = new AtomicBoolean();

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
        }

        private static void startScheduledCheckIfNeeded(ContainerCleanupProperties properties)
        {
            if (properties.getCheckIntervalSeconds() > 0 && SCHEDULER_STARTED.compareAndSet(false, true))
            {
                ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(runnable ->
                {
                    Thread thread = new Thread(runnable, "springtainer-cleanup-scheduler");
                    thread.setDaemon(true);
                    return thread;
                });

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
