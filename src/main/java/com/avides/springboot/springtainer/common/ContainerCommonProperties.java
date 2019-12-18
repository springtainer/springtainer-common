package com.avides.springboot.springtainer.common;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@ConfigurationProperties("embedded.container.common")
@Getter
@Setter
@ToString
public class ContainerCommonProperties
{
    private String network = "bridge";
}
