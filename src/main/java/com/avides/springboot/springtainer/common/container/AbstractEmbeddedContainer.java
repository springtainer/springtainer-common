package com.avides.springboot.springtainer.common.container;

import java.lang.reflect.Field;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.commons.lang.StringUtils;
import org.springframework.core.env.ConfigurableEnvironment;

import com.avides.springboot.springtainer.common.util.OSUtils;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DefaultDockerClientConfig.Builder;

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
        if (StringUtils.isNotBlank(getDockerHost()))
        {
            return getDockerHost();
        }

        if (OSUtils.isMac())
        {
            return environment.getProperty("embedded.container.mac.localhost.host", "127.0.0.1");
        }

        String containerNetwork = environment.getProperty("embedded.container.container-network", "bridge");
        return containerInfo.getNetworkSettings().getNetworks().get(containerNetwork).getIpAddress();
    }

    // See https://github.com/docker-java/docker-java/issues/1167 for further explanations
    @SneakyThrows(URISyntaxException.class)
    private static String getDockerHost()
    {
        try
        {
            Builder builder = DefaultDockerClientConfig.createDefaultConfigBuilder();
            Field declaredField = builder.getClass().getDeclaredField("dockerHost");
            declaredField.setAccessible(true);
            return ((URI) declaredField.get(builder)).getHost();
        }
        catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e)
        {
            log.warn("Unable to resolve the dockerHost by the DefaultDockerClientConfig. Switching to env variables..", e);
            String dockerHostProperty = System.getProperty("DOCKER_HOST", System.getenv("DOCKER_HOST"));
            return StringUtils.isNotBlank(dockerHostProperty) ? new URI(dockerHostProperty).getHost() : null;
        }
    }

    protected int getContainerPort(int exposed)
    {
        if (StringUtils.isNotBlank(getDockerHost()))
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
