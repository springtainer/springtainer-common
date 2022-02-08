package com.avides.springboot.springtainer.common.cleanup;

import static com.avides.springboot.springtainer.common.Labels.SPRINGTAINER_ISSUER;
import static com.avides.springboot.springtainer.common.Labels.SPRINGTAINER_STARTED;

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

import com.avides.springboot.springtainer.common.util.IssuerUtil;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.core.DockerClientBuilder;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Configuration
@ConditionalOnProperty(name = "embedded.container.cleanup.enabled", matchIfMissing = true)
@EnableConfigurationProperties(ContainerCleanupProperties.class)
@Slf4j
public class EmbeddedContainerCleanupAutoConfiguration
{
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
        }

        @SneakyThrows
        private int removeStaleContainers(ContainerCleanupProperties properties)
        {
            String currentIssuer = IssuerUtil.getIssuer();
            List<Container> issuerContainers = new ArrayList<>();
            List<Container> staleContainers = new ArrayList<>();

            try (DockerClient dockerClient = DockerClientBuilder.getInstance().build())
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

                if (issuerContainers.size() > properties.getMaxConcurrentPerIssuer())
                {
                    staleContainers.addAll(issuerContainers);
                    log.warn("Too much concurrent containers ({}) for issuer \"{}\"", Integer.valueOf(issuerContainers.size()), currentIssuer);
                }

                for (Container staleContainer : staleContainers)
                {
                    dockerClient.removeContainerCmd(staleContainer.getId()).withForce(Boolean.TRUE).withRemoveVolumes(Boolean.TRUE).exec();
                    log.warn("Stale container removed ({})", staleContainer.labels);
                }
            }

            return staleContainers.size();
        }
    }
}
