package com.avides.springboot.springtainer.common.util;

import lombok.experimental.UtilityClass;

@UtilityClass
public class OSUtils
{
    public boolean isMac()
    {
        return System.getProperty("os.name").toLowerCase().contains("mac");
    }

    public boolean isLinux()
    {
        return System.getProperty("os.name").toLowerCase().contains("linux");
    }
}
