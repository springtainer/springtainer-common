package com.avides.springboot.testcontainer.common;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@ConfigurationProperties("embedded.container")
@Getter
@Setter
@ToString
public class CommonEmbeddedContainerProperties
{
    private String containerNetwork = "bridge";

    private boolean cleanupEnabled = true;

    private int cleanupAfterMinutes = 10;
}
