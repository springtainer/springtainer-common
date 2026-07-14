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
    private static volatile DockerClient shared;

    /**
     * Returns a {@link DockerClient} shared for the lifetime of this JVM, building it lazily on first use.
     * <p>
     * A fresh {@link ApacheDockerHttpClient} sets up its own connection pool on every {@link #build()} call, which is real, avoidable overhead when done
     * once per container start/stop/cleanup-check rather than once per JVM - this is docker-java's own recommended usage pattern.
     * <p>
     * Deliberately never closed: several independent JVM shutdown hooks (Spring's test-context cache eviction, and this library's own cleanup shutdown
     * hook) may still need to use this client while the JVM is exiting, and Java gives no ordering guarantee between shutdown hooks - closing it from a
     * hook of its own could race with, and break, those other hooks. Leaving it open costs nothing: the JVM is exiting anyway and the OS reclaims the
     * underlying sockets/threads regardless.
     *
     * @return the shared {@link DockerClient}
     */
    public static DockerClient shared()
    {
        DockerClient result = shared;

        if (result == null)
        {
            synchronized (DockerClients.class)
            {
                result = shared;

                if (result == null)
                {
                    result = build();
                    shared = result;
                }
            }
        }

        return result;
    }

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
