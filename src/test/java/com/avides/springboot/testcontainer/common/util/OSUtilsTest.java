package com.avides.springboot.testcontainer.common.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class OSUtilsTest
{
    private String osName;

    @Before
    public void before()
    {
        osName = System.getProperty("os.name");
    }

    @After
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
