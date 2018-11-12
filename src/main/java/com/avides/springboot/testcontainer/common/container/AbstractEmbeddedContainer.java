package com.avides.springboot.testcontainer.common.container;

import org.springframework.core.env.ConfigurableEnvironment;

import com.avides.springboot.testcontainer.common.OSUtils;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.ExposedPort;

public abstract class AbstractEmbeddedContainer<P extends AbstractEmbeddedContainerProperties> implements EmbeddedContainer
{
    protected ConfigurableEnvironment environment;

    protected P properties;

    protected InspectContainerResponse containerInfo;

    protected String getContainerHost()
    {
        if (OSUtils.isMac())
        {
            return "localhost";
        }

        String containerNetwork = environment.getProperty("embedded.container.container-network", "bridge");
        return containerInfo.getNetworkSettings().getNetworks().get(containerNetwork).getIpAddress();
    }

    protected int getContainerPort(int exposed)
    {
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
        dockerClient.killContainerCmd(containerInfo.getId()).exec();
    }
}
