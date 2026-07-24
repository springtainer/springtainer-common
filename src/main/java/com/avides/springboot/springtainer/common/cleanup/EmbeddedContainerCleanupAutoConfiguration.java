package com.avides.springboot.springtainer.common.cleanup;

import static com.avides.springboot.springtainer.common.Labels.SPRINGTAINER_ISSUER;
import static com.avides.springboot.springtainer.common.Labels.SPRINGTAINER_STARTED;
import static java.util.Comparator.comparingLong;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.avides.springboot.springtainer.common.util.DockerClients;
import com.avides.springboot.springtainer.common.util.IssuerUtil;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Container;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Configuration
@ConditionalOnProperty(name = "embedded.container.cleanup.enabled", matchIfMissing = true)
@EnableConfigurationProperties(ContainerCleanupProperties.class)
@Slf4j
public class EmbeddedContainerCleanupAutoConfiguration
{
    @ConditionalOnMissingBean(EmbeddedContainerCleanup.class)
    @Bean
    EmbeddedContainerCleanup embeddedContainerCleanup(ContainerCleanupProperties properties)
    {
        return new EmbeddedContainerCleanup(properties);
    }

    @RequiredArgsConstructor
    public static class EmbeddedContainerCleanup
    {
        public EmbeddedContainerCleanup(ContainerCleanupProperties properties)
        {
            log.info("{} stale containers removed", Integer.valueOf(removeStaleContainers(properties)));
        }

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
                dockerClient.removeContainerCmd(staleContainer.getId()).withForce(Boolean.TRUE).withRemoveVolumes(Boolean.TRUE).exec();
                log.warn("Stale container removed ({})", staleContainer.labels);
            }

            return staleContainers.size();
        }
    }
}
