package com.avides.springboot.springtainer.common.cleanup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class ContainerCleanupPropertiesTest
{
    @Test
    public void testDefaults()
    {
        ContainerCleanupProperties properties = new ContainerCleanupProperties();
        assertTrue(properties.isEnabled());
        assertEquals(10, properties.getAfterMinutes());
        assertEquals(10, properties.getMaxConcurrentPerIssuer());
        assertEquals(0, properties.getCheckIntervalSeconds());
    }
}
