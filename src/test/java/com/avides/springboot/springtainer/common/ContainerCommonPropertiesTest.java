package com.avides.springboot.springtainer.common;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class ContainerCommonPropertiesTest
{
    @Test
    public void testDefaults()
    {
        ContainerCommonProperties properties = new ContainerCommonProperties();
        assertEquals("bridge", properties.getNetwork());
    }
}
