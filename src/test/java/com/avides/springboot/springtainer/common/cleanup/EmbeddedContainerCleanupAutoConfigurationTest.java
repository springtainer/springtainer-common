package com.avides.springboot.springtainer.common.cleanup;

import static com.avides.springboot.springtainer.common.Labels.SPRINGTAINER_ISSUER;
import static com.avides.springboot.springtainer.common.Labels.SPRINGTAINER_STARTED;
import static java.time.Duration.ofMinutes;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import com.avides.springboot.springtainer.common.cleanup.EmbeddedContainerCleanupAutoConfiguration.EmbeddedContainerCleanup;
import com.avides.springboot.springtainer.common.util.DockerClients;
import com.avides.springboot.springtainer.common.util.IssuerUtil;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Container;

public class EmbeddedContainerCleanupAutoConfigurationTest
{
    private static Container containerWithLabels(String id, Map<String, String> labels)
    {
        Container container = mock(Container.class);
        when(container.getId()).thenReturn(id);
        when(container.getLabels()).thenReturn(labels);
        return container;
    }

    private static Map<String, String> labels(long startedAgo, String issuer)
    {
        return Map.of(SPRINGTAINER_STARTED, String.valueOf(System.currentTimeMillis() - startedAgo), SPRINGTAINER_ISSUER, issuer);
    }

    @Test
    public void testRemovesContainersOlderThanAfterMinutes()
    {
        DockerClient dockerClient = mock(DockerClient.class, RETURNS_DEEP_STUBS);
        Container fresh = containerWithLabels("fresh", labels(ofMinutes(1).toMillis(), IssuerUtil.getIssuer()));
        Container stale = containerWithLabels("stale", labels(ofMinutes(11).toMillis(), IssuerUtil.getIssuer()));
        when(dockerClient.listContainersCmd().withLabelFilter(List.of(SPRINGTAINER_STARTED)).exec()).thenReturn(List.of(fresh, stale));

        try (MockedStatic<DockerClients> dockerClients = mockStatic(DockerClients.class))
        {
            dockerClients.when(DockerClients::shared).thenReturn(dockerClient);

            new EmbeddedContainerCleanup(new ContainerCleanupProperties());

            verify(dockerClient).removeContainerCmd("stale");
            verify(dockerClient, never()).removeContainerCmd("fresh");
        }
    }

    @Test
    public void testRemovesOnlyOldestExcessContainersWhenOverMaxConcurrentPerIssuer()
    {
        DockerClient dockerClient = mock(DockerClient.class, RETURNS_DEEP_STUBS);
        String issuer = IssuerUtil.getIssuer();

        Container oldest = containerWithLabels("oldest", labels(30_000, issuer));
        Container middle = containerWithLabels("middle", labels(20_000, issuer));
        Container newest = containerWithLabels("newest", labels(10_000, issuer));
        when(dockerClient.listContainersCmd().withLabelFilter(List.of(SPRINGTAINER_STARTED)).exec()).thenReturn(List.of(newest, oldest, middle));

        ContainerCleanupProperties properties = new ContainerCleanupProperties();
        properties.setMaxConcurrentPerIssuer(2);

        try (MockedStatic<DockerClients> dockerClients = mockStatic(DockerClients.class))
        {
            dockerClients.when(DockerClients::shared).thenReturn(dockerClient);

            new EmbeddedContainerCleanup(properties);

            // 3 containers for this issuer > maxConcurrentPerIssuer of 2: only the single oldest excess container is removed, not all of them - a
            // just-started container is very likely still in active use by the test that created it.
            verify(dockerClient).removeContainerCmd("oldest");
            verify(dockerClient, never()).removeContainerCmd("middle");
            verify(dockerClient, never()).removeContainerCmd("newest");
        }
    }

    @Test
    public void testIgnoresContainersFromOtherIssuersForTheConcurrencyCheck()
    {
        DockerClient dockerClient = mock(DockerClient.class, RETURNS_DEEP_STUBS);

        Container ownFirst = containerWithLabels("ownFirst", labels(20_000, IssuerUtil.getIssuer()));
        Container ownSecond = containerWithLabels("ownSecond", labels(10_000, IssuerUtil.getIssuer()));
        Container otherIssuer = containerWithLabels("otherIssuer", labels(30_000, "some-other-module"));
        when(dockerClient.listContainersCmd().withLabelFilter(List.of(SPRINGTAINER_STARTED)).exec()).thenReturn(List.of(ownFirst, ownSecond, otherIssuer));

        ContainerCleanupProperties properties = new ContainerCleanupProperties();
        properties.setMaxConcurrentPerIssuer(2);

        try (MockedStatic<DockerClients> dockerClients = mockStatic(DockerClients.class))
        {
            dockerClients.when(DockerClients::shared).thenReturn(dockerClient);

            new EmbeddedContainerCleanup(properties);

            verify(dockerClient, never()).removeContainerCmd(anyString());
        }
    }
}
