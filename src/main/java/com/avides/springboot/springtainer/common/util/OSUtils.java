package com.avides.springboot.springtainer.common.util;

import static java.lang.System.getProperty;
import static lombok.AccessLevel.PRIVATE;

import lombok.NoArgsConstructor;

@NoArgsConstructor(access = PRIVATE)
public class OSUtils
{
    public static boolean isMac()
    {
        return getProperty("os.name").toLowerCase().contains("mac");
    }

    public static boolean isLinux()
    {
        return getProperty("os.name").toLowerCase().contains("linux");
    }
}
