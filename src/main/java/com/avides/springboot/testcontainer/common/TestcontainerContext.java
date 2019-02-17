package com.avides.springboot.testcontainer.common;

import org.apache.commons.lang.StringUtils;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DockerClientBuilder;

import lombok.Getter;

public class TestcontainerContext
{
    @Getter
    private static String osType;

    public static DockerClient createDockerClient()
    {
        DockerClient dockerClient = DockerClientBuilder.getInstance().build();

        if (StringUtils.isBlank(osType))
        {
            osType = dockerClient.infoCmd().exec().getOsType();
        }

        return dockerClient;
    }

    public static boolean isRunningOnLinux()
    {
        return "linux".equalsIgnoreCase(osType);
    }
}
