package com.avides.springboot.springtainer.common.util;

import static lombok.AccessLevel.PRIVATE;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;

import lombok.NoArgsConstructor;

/**
 * Builds a {@link DockerClient} with an explicitly configured {@link ApacheDockerHttpClient}.
 * <p>
 * Without an explicit {@link com.github.dockerjava.transport.DockerHttpClient DockerHttpClient}, docker-java silently falls back to its legacy Jersey
 * transport (logging "'dockerHttpClient' should be set. Falling back to Jersey, will be an error in future releases."), which future docker-java releases
 * will turn into a hard failure.
 */
@NoArgsConstructor(access = PRIVATE)
public final class DockerClients
{
    public static DockerClient build()
    {
        var config = DefaultDockerClientConfig.createDefaultConfigBuilder().build();

        var httpClient = new ApacheDockerHttpClient.Builder()
                .dockerHost(config.getDockerHost())
                .sslConfig(config.getSSLConfig())
                .build();

        return DockerClientBuilder.getInstance(config).withDockerHttpClient(httpClient).build();
    }
}
