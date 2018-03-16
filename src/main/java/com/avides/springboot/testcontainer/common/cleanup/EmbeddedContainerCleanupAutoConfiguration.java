package com.avides.springboot.testcontainer.common.cleanup;

import static com.avides.springboot.testcontainer.common.Labels.TESTCONTAINER_STARTED;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

import com.avides.springboot.testcontainer.common.CommonEmbeddedContainerProperties;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.core.DockerClientBuilder;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Configuration
@ConditionalOnProperty(name = "embedded.container.cleanup.enabled", matchIfMissing = true)
@AutoConfigureOrder(Ordered.LOWEST_PRECEDENCE)
@EnableConfigurationProperties(CommonEmbeddedContainerProperties.class)
@Slf4j
public class EmbeddedContainerCleanupAutoConfiguration
{
    @ConditionalOnMissingBean(EmbeddedContainerCleanup.class)
    @Bean
    public EmbeddedContainerCleanup embeddedContainerCleanup(CommonEmbeddedContainerProperties properties)
    {
        return new EmbeddedContainerCleanup(properties);
    }

    @RequiredArgsConstructor
    public class EmbeddedContainerCleanup
    {
        public EmbeddedContainerCleanup(CommonEmbeddedContainerProperties properties)
        {
            log.info("{} stale containers removed", Integer.valueOf(removeStaleContainers(properties)));
        }

        @SneakyThrows
        private int removeStaleContainers(CommonEmbeddedContainerProperties properties)
        {
            int removed = 0;

            try (DockerClient dockerClient = DockerClientBuilder.getInstance().build())
            {
                for (Container container : dockerClient.listContainersCmd().exec())
                {
                    if (container.getLabels().containsKey(TESTCONTAINER_STARTED))
                    {
                        long millis = Long.parseLong(container.getLabels().get(TESTCONTAINER_STARTED));
                        LocalDateTime started = LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault());
                        LocalDateTime staleSince = LocalDateTime.now().minusMinutes(properties.getCleanupAfterMinutes());

                        if (started.isBefore(staleSince))
                        {
                            log.info("Removing stale container ({})", container.getId());
                            dockerClient.stopContainerCmd(container.getId()).exec();
                            dockerClient.removeContainerCmd(container.getId()).exec();
                            log.info("Stale container removed ({})", container.getId());
                            removed++;
                        }
                    }
                }
            }

            return removed;
        }
    }
}
