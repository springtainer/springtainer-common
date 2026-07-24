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
    /**
     * Initialization-on-demand holder: the JVM only initializes {@code Holder} (and thus builds {@link #INSTANCE}) the first time {@link #shared()} is
     * called, giving lazy, thread-safe, build-once semantics via the class-loading guarantee alone - no {@code volatile}/{@code synchronized} needed.
     */
    private static final class Holder
    {
        private static final DockerClient INSTANCE = build();
    }

    /**
     * Returns a {@link DockerClient} shared for the lifetime of this JVM, building it lazily on first use.
     * <p>
     * A fresh {@link ApacheDockerHttpClient} sets up its own connection pool on every {@link #build()} call, which is real, avoidable overhead when done
     * once per container start/stop/cleanup-check rather than once per JVM - this is docker-java's own recommended usage pattern.
     * <p>
     * Deliberately never closed: Spring's test-context cache may still need to use this client from its own shutdown hook while the JVM is exiting, and
     * Java gives no ordering guarantee between shutdown hooks - closing it from a hook of its own could race with, and break, that other hook. Leaving it
     * open costs nothing: the JVM is exiting anyway and the OS reclaims the underlying sockets/threads regardless.
     *
     * @return the shared {@link DockerClient}
     */
    public static DockerClient shared()
    {
        return Holder.INSTANCE;
    }

    /**
     * Builds a fresh {@link DockerClient}, independent of the JVM-wide {@link #shared()} instance. Used directly by every springtainer module's
     * integration tests (see each module's {@code AbstractIT}) to inspect/manipulate containers without touching the instance production code relies on.
     *
     * @return a new {@link DockerClient}
     */
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
