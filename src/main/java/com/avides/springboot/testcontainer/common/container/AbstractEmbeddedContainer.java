package com.avides.springboot.testcontainer.common.container;

import java.net.URI;

import org.apache.commons.lang.StringUtils;
import org.springframework.core.env.ConfigurableEnvironment;

import com.avides.springboot.testcontainer.common.util.OSUtils;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.ExposedPort;

import lombok.SneakyThrows;

public abstract class AbstractEmbeddedContainer<P extends AbstractEmbeddedContainerProperties> implements EmbeddedContainer
{
    protected ConfigurableEnvironment environment;

    protected P properties;

    protected InspectContainerResponse containerInfo;

    @SneakyThrows
    protected String getContainerHost()
    {
        if (StringUtils.isNotBlank(getRemoteHost()))
        {
            return new URI(getRemoteHost()).getHost();
        }

        if (OSUtils.isMac())
        {
            return "localhost";
        }

        String containerNetwork = environment.getProperty("embedded.container.container-network", "bridge");
        return containerInfo.getNetworkSettings().getNetworks().get(containerNetwork).getIpAddress();
    }

    private static String getRemoteHost()
    {
        return System.getProperty("DOCKER_HOST", System.getenv("DOCKER_HOST"));
    }

    protected int getContainerPort(int exposed)
    {
        if (getRemoteHost() != null)
        {
            return getMappedPort(exposed);
        }

        if (OSUtils.isMac())
        {
            return getMappedPort(exposed);
        }

        return exposed;
    }

    private int getMappedPort(int exposed)
    {
        return Integer.parseInt(containerInfo.getNetworkSettings().getPorts().getBindings().get(new ExposedPort(exposed))[0].getHostPortSpec());
    }

    protected void killContainer(DockerClient dockerClient)
    {
        dockerClient.removeContainerCmd(containerInfo.getId()).withForce(Boolean.TRUE).withRemoveVolumes(Boolean.TRUE).exec();
    }
}
