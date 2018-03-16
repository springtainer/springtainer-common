package com.avides.springboot.testcontainer.common.container;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public abstract class AbstractEmbeddedContainerProperties
{
    private boolean enabled = true;

    private int startupTimeout = 30;

    private String dockerImage;
}
