package com.avides.springboot.testcontainer.common.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

import lombok.experimental.UtilityClass;

@UtilityClass
public class IssuerUtil
{
    private static final Pattern TARGET_PATTERN = Pattern.compile("(.*)(/|\\\\)(.*)(/|\\\\)target(/|\\\\)(.*)");

    public String getIssuer()
    {
        String javaCommand = System.getProperty("sun.java.command");

        if (StringUtils.isNotBlank(javaCommand) && javaCommand.contains("/target/"))
        {
            Matcher matcher = TARGET_PATTERN.matcher(javaCommand);

            if (matcher.find() && matcher.groupCount() > 3)
            {
                return matcher.group(3);
            }
        }

        String javaClassPath = System.getProperty("java.class.path");

        if (StringUtils.isNotBlank(javaClassPath) && javaClassPath.contains("/target/"))
        {
            Matcher matcher = TARGET_PATTERN.matcher(javaClassPath);

            if (matcher.find() && matcher.groupCount() > 3)
            {
                return matcher.group(3);
            }
        }

        return "unknown";
    }
}
