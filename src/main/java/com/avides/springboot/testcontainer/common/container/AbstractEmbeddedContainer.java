package com.avides.springboot.testcontainer.common.container;

import java.net.URI;

import org.apache.commons.lang.StringUtils;
import org.springframework.core.env.ConfigurableEnvironment;

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
        String remoteHost = getRemoteHost();

        if (StringUtils.isNotBlank(remoteHost))
        {
            return new URI(remoteHost).getHost();
        }

        String containerNetwork = environment.getProperty("embedded.container.container-network", "bridge");
        return containerInfo.getNetworkSettings().getNetworks().get(containerNetwork).getIpAddress();
    }

    protected int getContainerPort(int exposed)
    {
        if (getRemoteHost() != null)
        {
            return Integer.parseInt(containerInfo.getNetworkSettings().getPorts().getBindings().get(new ExposedPort(exposed))[0].getHostPortSpec());
        }

        return exposed;
    }

    private static String getRemoteHost()
    {
        return System.getProperty("DOCKER_HOST", System.getenv("DOCKER_HOST"));
    }

    protected void killContainer(DockerClient dockerClient)
    {
        dockerClient.removeContainerCmd(containerInfo.getId()).withForce(Boolean.TRUE).withRemoveVolumes(Boolean.TRUE).exec();
    }
}
