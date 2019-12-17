package com.avides.springboot.springtainer.common.cleanup;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@ConfigurationProperties("embedded.container.cleanup")
@Getter
@Setter
@ToString
public class ContainerCleanupProperties
{
    private boolean enabled = true;

    private int afterMinutes = 10;

    private int maxConcurrentPerIssuer = 10;
}
