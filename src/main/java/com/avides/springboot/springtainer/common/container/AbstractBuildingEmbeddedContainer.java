package com.avides.springboot.springtainer.common.container;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.rnorth.ducttape.TimeoutException;
import org.rnorth.ducttape.unreliables.Unreliables;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ApplicationContextEvent;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextStoppedEvent;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import com.avides.springboot.springtainer.common.Labels;
import com.avides.springboot.springtainer.common.util.IssuerUtil;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.PullImageResultCallback;
import com.github.dockerjava.api.exception.InternalServerErrorException;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.core.DockerClientBuilder;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractBuildingEmbeddedContainer<P extends AbstractEmbeddedContainerProperties> extends AbstractEmbeddedContainer<P>
        implements ApplicationListener<ApplicationContextEvent>
{
    protected String service;

    @SneakyThrows
    protected AbstractBuildingEmbeddedContainer(String service, ConfigurableEnvironment environment, P properties)
    {
        this.service = service;
        this.environment = environment;
        this.properties = properties;

        log.info("Starting {}-container with {}", service, properties);

        try (DockerClient dockerClient = DockerClientBuilder.getInstance().build())
        {
            createContainer(dockerClient);

            log.info("Checking {}-container... (Timeout: {}s)", service, Integer.valueOf(properties.getStartupTimeout()));
            long startupDuration = waitUntilReady(properties);
            log.info("{}-container started (Duration: {}ms, Host: {})", service, Long.valueOf(startupDuration), getContainerHost());

            environment.getPropertySources().addFirst(new MapPropertySource("embedded" + service + "Properties", providedProperties()));
        }
        catch (NotFoundException e)
        {
            if (e.getMessage().contains("No such image"))
            {
                log.error("Image not found ({})", properties.getDockerImage());
            }
            else
            {
                log.error("Unknown error pulling image", e);
            }
        }
        catch (ContainerStartupFailedException e)
        {
            killContainer(DockerClientBuilder.getInstance().build());
            log.error("Failed to start {}-container", service, e);
        }
    }

    protected List<String> getEnvs()
    {
        return new ArrayList<>();
    }

    protected Map<String, String> getLabels()
    {
        return new HashMap<>();
    }

    /**
     * Configures a list of directories which can be mapped to memory (tmpfs)
     * <p>
     * Normally all VOLUMEs can be used (Exclude huge volumes!)
     * <p>
     * This feature works only on linux!
     *
     * @return List of directories
     */
    protected List<String> getTmpDirectories()
    {
        return new ArrayList<>();
    }

    private Map<String, String> getSpringtainerLabels()
    {
        Map<String, String> labels = new HashMap<>();
        labels.put(Labels.SPRINGTAINER_SERVICE, service);
        labels.put(Labels.SPRINGTAINER_IMAGE, properties.getDockerImage());
        labels.put(Labels.SPRINGTAINER_STARTED, String.valueOf(System.currentTimeMillis()));
        labels.put(Labels.SPRINGTAINER_ISSUER, IssuerUtil.getIssuer());
        return labels;
    }

    private Map<String, String> getAllLabels()
    {
        Map<String, String> labels = getLabels();
        labels.putAll(getSpringtainerLabels());
        return labels;
    }

    /**
     * Builds {@link HostConfig}
     * <p>
     * There is normally no need to overwrite this method!<br>
     * If you need to overwrite this method, make sure that all ports are published!
     *
     * @return configured {@link HostConfig}
     */
    protected HostConfig buildHostConfig()
    {
        HostConfig hostConfig = new HostConfig();
        hostConfig.withPublishAllPorts(Boolean.TRUE);

        if (!getTmpDirectories().isEmpty())
        {
            Map<String, String> tmpDirectories = new HashMap<>();
            getTmpDirectories().forEach(directory -> tmpDirectories.put(directory, ""));
            hostConfig.withTmpFs(tmpDirectories);
        }

        return hostConfig;
    }

    protected void createContainer(DockerClient dockerClient) throws InterruptedException
    {
        pullImage(dockerClient);
        CreateContainerCmd createContainerCmd = dockerClient.createContainerCmd(properties.getDockerImage()) // NOSONAR
                .withLabels(getAllLabels())
                .withEnv(getEnvs())
                .withHostConfig(buildHostConfig());

        adjustCreateCommand(createContainerCmd);
        String containerId = createContainerCmd.exec().getId();
        dockerClient.startContainerCmd(containerId).exec();
        containerInfo = dockerClient.inspectContainerCmd(containerId).exec();
    }

    /**
     * Adjust prepared {@link CreateContainerCmd} before create
     *
     * @param createContainerCmd prepared command
     */
    protected void adjustCreateCommand(CreateContainerCmd createContainerCmd)
    {
        // nothing
    }

    protected void pullImage(DockerClient dockerClient) throws InterruptedException
    {
        try
        {
            // Pulling from remote is always slower than pulling from local
            try
            {
                dockerClient.inspectImageCmd(properties.getDockerImage()).exec();
            }
            catch (NotFoundException e)
            {
                dockerClient.pullImageCmd(properties.getDockerImage()).exec(new PullImageResultCallback()).awaitCompletion();
            }
        }
        // Ignore registry-problems and try to proceed with local image
        catch (InternalServerErrorException e)
        {
            log.warn("Failed to pull image from registry. Try to proceed with local image..", e);
        }
    }

    protected Map<String, Object> providedProperties()
    {
        return new HashMap<>();
    }

    protected long waitUntilReady(P properties) throws ContainerStartupFailedException
    {
        try
        {
            long started = System.currentTimeMillis();
            Unreliables.retryUntilTrue(properties.getStartupTimeout(), TimeUnit.SECONDS, () -> Boolean.valueOf(isContainerReady(properties)));
            return System.currentTimeMillis() - started;
        }
        catch (TimeoutException e)
        {
            throw new ContainerStartupFailedException(e);
        }
    }

    protected abstract boolean isContainerReady(P properties);

    @SneakyThrows
    @Override
    public void onApplicationEvent(ApplicationContextEvent event)
    {
        if (event instanceof ContextStoppedEvent || event instanceof ContextClosedEvent)
        {
            try (DockerClient dockerClient = DockerClientBuilder.getInstance().build())
            {
                log.info("Stopping {}-container...", service);
                killContainer(dockerClient);
                log.info("{}-container stopped", service);
            }
            catch (NotFoundException e)
            {
                log.info("{}-container not found.. ignored", service, e);
            }
        }
    }
}
