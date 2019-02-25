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
        String dockerHost = System.getProperty("DOCKER_HOST", System.getenv("DOCKER_HOST"));

        if (StringUtils.isNotBlank(dockerHost))
        {
            return new URI(dockerHost).getHost();
        }

        String containerNetwork = environment.getProperty("embedded.container.container-network", "bridge");
        return containerInfo.getNetworkSettings().getNetworks().get(containerNetwork).getIpAddress();
    }

    protected int getContainerPort(int exposed)
    {
        return Integer.parseInt(containerInfo.getNetworkSettings().getPorts().getBindings().get(new ExposedPort(exposed))[0].getHostPortSpec());
    }

    protected void killContainer(DockerClient dockerClient)
    {
        dockerClient.removeContainerCmd(containerInfo.getId()).withForce(Boolean.TRUE).withRemoveVolumes(Boolean.TRUE).exec();
    }
}
