package com.avides.springboot.testcontainer.common.cleanup;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ContainerCleanupPropertiesTest
{
    @Test
    public void testDefaults()
    {
        ContainerCleanupProperties properties = new ContainerCleanupProperties();
        assertTrue(properties.isEnabled());
        assertEquals(10, properties.getAfterMinutes());
        assertEquals(10, properties.getMaxConcurrentPerIssuer());
    }
}
