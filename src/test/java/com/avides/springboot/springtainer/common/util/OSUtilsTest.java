package com.avides.springboot.springtainer.common.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class OSUtilsTest
{
    private String osName;

    @BeforeEach
    public void before()
    {
        osName = System.getProperty("os.name");
    }

    @AfterEach
    public void after()
    {
        System.setProperty("os.name", osName);
    }

    @Test
    public void testIsMacWithMac()
    {
        System.setProperty("os.name", "MAC OS 11.12");
        assertTrue(OSUtils.isMac());
    }

    @Test
    public void testIsMacWithLinux()
    {
        System.setProperty("os.name", "Ubuntu Linux 16.04 LTS");
        assertFalse(OSUtils.isMac());
    }

    @Test
    public void testIsLinuxWithMac()
    {
        System.setProperty("os.name", "MAC OS 11.12");
        assertFalse(OSUtils.isLinux());
    }

    @Test
    public void testIsLinuxWithLinux()
    {
        System.setProperty("os.name", "Ubuntu Linux 16.04 LTS");
        assertTrue(OSUtils.isLinux());
    }
}
