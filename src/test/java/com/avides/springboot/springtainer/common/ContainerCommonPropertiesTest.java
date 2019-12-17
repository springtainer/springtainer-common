package com.avides.springboot.springtainer.common;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.avides.springboot.springtainer.common.ContainerCommonProperties;

public class ContainerCommonPropertiesTest
{
    @Test
    public void testDefaults()
    {
        ContainerCommonProperties properties = new ContainerCommonProperties();
        assertEquals("bridge", properties.getNetwork());
    }
}
