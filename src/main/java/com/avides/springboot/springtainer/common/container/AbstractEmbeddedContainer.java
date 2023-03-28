package com.avides.springboot.springtainer.common.container;

import static com.avides.springboot.springtainer.common.util.OSUtils.isMac;
import static java.lang.Boolean.TRUE;
import static java.lang.Integer.parseInt;
import static java.lang.System.getProperty;
import static java.lang.System.getenv;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.net.URI;
import java.net.URISyntaxException;

import org.springframework.core.env.ConfigurableEnvironment;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.core.DefaultDockerClientConfig;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractEmbeddedContainer<P extends AbstractEmbeddedContainerProperties> implements EmbeddedContainer
{
    protected ConfigurableEnvironment environment;

    protected P properties;

    protected InspectContainerResponse containerInfo;

    protected String getContainerHost()
    {
        if (isNotBlank(getDockerHost()))
        {
            return getDockerHost();
        }

        if (isMac())
        {
            return environment.getProperty("embedded.container.mac.localhost.host", "127.0.0.1");
        }

        var containerNetwork = environment.getProperty("embedded.container.container-network", "bridge");
        return containerInfo.getNetworkSettings().getNetworks().get(containerNetwork).getIpAddress();
    }

    @SneakyThrows(URISyntaxException.class)
    private static String getDockerHost()
    {
        try
        {
            return DefaultDockerClientConfig.createDefaultConfigBuilder().build().getDockerHost().getHost();
        }
        catch (IllegalArgumentException | SecurityException e)
        {
            log.warn("Unable to resolve the dockerHost by the DefaultDockerClientConfig. Switching to env variables..", e);
            var dockerHostProperty = getProperty("DOCKER_HOST", getenv("DOCKER_HOST"));
            return isNotBlank(dockerHostProperty) ? new URI(dockerHostProperty).getHost() : null;
        }
    }

    protected int getContainerPort(int exposed)
    {
        if (isNotBlank(getDockerHost()))
        {
            return getMappedPort(exposed);
        }

        if (isMac())
        {
            return getMappedPort(exposed);
        }

        return exposed;
    }

    private int getMappedPort(int exposed)
    {
        return parseInt(containerInfo.getNetworkSettings().getPorts().getBindings().get(new ExposedPort(exposed))[0].getHostPortSpec());
    }

    @SuppressWarnings("resource")
    protected void killContainer(DockerClient dockerClient)
    {
        dockerClient.removeContainerCmd(containerInfo.getId()).withForce(TRUE).withRemoveVolumes(TRUE).exec();
    }
}
