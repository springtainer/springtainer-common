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
        // https://wiki.jenkins.io/display/JENKINS/Building+a+software+project#Buildingasoftwareproject-belowJenkinsSetEnvironmentVariables
        String jobName = EnvProvider.getEnv("JOB_NAME");

        if (StringUtils.isNotBlank(jobName))
        {
            return jobName;
        }

        String javaCommand = System.getProperty("sun.java.command");

        if (StringUtils.isNotBlank(javaCommand))
        {
            Matcher matcher = TARGET_PATTERN.matcher(javaCommand.split(":")[0]);

            if (matcher.find())
            {
                return matcher.group(3);
            }
        }

        String javaClassPath = System.getProperty("java.class.path");

        if (StringUtils.isNotBlank(javaClassPath))
        {
            Matcher matcher = TARGET_PATTERN.matcher(javaClassPath.split(":")[0]);

            if (matcher.find())
            {
                return matcher.group(3);
            }
        }

        return "unknown";
    }

    @UtilityClass
    public static class EnvProvider
    {
        public static String getEnv(String key)
        {
            return System.getenv(key);
        }
    }
}
