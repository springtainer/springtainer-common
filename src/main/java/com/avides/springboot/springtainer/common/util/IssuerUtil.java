package com.avides.springboot.springtainer.common.util;

import static java.lang.System.getProperty;
import static java.lang.System.setProperty;
import static lombok.AccessLevel.PRIVATE;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.util.regex.Pattern;

import lombok.NoArgsConstructor;

@NoArgsConstructor(access = PRIVATE)
public class IssuerUtil
{
    private static final Pattern TARGET_PATTERN = Pattern.compile("(.*)(/|\\\\)(.*)(/|\\\\)target(/|\\\\)(.*)");

    static
    {
        // Transform env to property to simplify testing
        var jobName = System.getenv("JOB_NAME");

        if (isNotBlank(jobName))
        {
            setProperty("JENKINS_JOB_NAME", jobName);
        }
    }

    public static String getIssuer()
    {
        // https://wiki.jenkins.io/display/JENKINS/Building+a+software+project#Buildingasoftwareproject-belowJenkinsSetEnvironmentVariables
        var jobName = getProperty("JENKINS_JOB_NAME");

        if (isNotBlank(jobName))
        {
            return jobName;
        }

        var javaCommand = getProperty("sun.java.command");

        if (isNotBlank(javaCommand))
        {
            var matcher = TARGET_PATTERN.matcher(javaCommand.split(":")[0]);

            if (matcher.find())
            {
                return matcher.group(3);
            }
        }

        var javaClassPath = getProperty("java.class.path");

        if (isNotBlank(javaClassPath))
        {
            var matcher = TARGET_PATTERN.matcher(javaClassPath.split(":")[0]);

            if (matcher.find())
            {
                return matcher.group(3);
            }
        }

        return "unknown";
    }
}
