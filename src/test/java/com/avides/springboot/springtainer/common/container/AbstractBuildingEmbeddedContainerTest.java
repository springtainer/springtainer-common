package com.avides.springboot.springtainer.common.container;

import static com.avides.springboot.springtainer.common.Labels.SPRINGTAINER_ISSUER;
import static com.avides.springboot.springtainer.common.Labels.SPRINGTAINER_SERVICE;
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
import org.springframework.mock.env.MockEnvironment;

import com.avides.springboot.springtainer.common.util.DockerClients;
import com.avides.springboot.springtainer.common.util.IssuerUtil;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Container;

public class AbstractBuildingEmbeddedContainerTest
{
    private static Container containerWithLabels(String id, Map<String, String> labels)
    {
        Container container = mock(Container.class);
        when(container.getId()).thenReturn(id);
        when(container.getLabels()).thenReturn(labels);
        return container;
    }

    private static TestProperties properties()
    {
        TestProperties properties = new TestProperties();
        properties.setDockerImage("test-image:latest");
        return properties;
    }

    @Test
    public void testStopsPreviousContainerForSameServiceAndIssuerBeforeCreatingNewOne()
    {
        DockerClient dockerClient = mock(DockerClient.class, RETURNS_DEEP_STUBS);
        String issuer = IssuerUtil.getIssuer();

        Container previousSameService = containerWithLabels("previous-same-service",
                Map.of(SPRINGTAINER_SERVICE, "test-service", SPRINGTAINER_ISSUER, issuer));
        Container otherService = containerWithLabels("other-service", Map.of(SPRINGTAINER_SERVICE, "other-service", SPRINGTAINER_ISSUER, issuer));
        Container otherIssuer = containerWithLabels("other-issuer", Map.of(SPRINGTAINER_SERVICE, "test-service", SPRINGTAINER_ISSUER, "some-other-module"));
        when(dockerClient.listContainersCmd().exec()).thenReturn(List.of(previousSameService, otherService, otherIssuer));

        try (MockedStatic<DockerClients> dockerClients = mockStatic(DockerClients.class))
        {
            dockerClients.when(DockerClients::build).thenReturn(dockerClient);

            new TestContainer("test-service", new MockEnvironment(), properties());

            // only the previous container for the SAME service+issuer is stopped - a sibling service (e.g. a different container type started for the
            // same test class) or a container belonging to a different module must never be touched
            verify(dockerClient).removeContainerCmd("previous-same-service");
            verify(dockerClient, never()).removeContainerCmd("other-service");
            verify(dockerClient, never()).removeContainerCmd("other-issuer");
        }
    }

    @Test
    public void testDoesNotFailWhenNoPreviousContainerExists()
    {
        DockerClient dockerClient = mock(DockerClient.class, RETURNS_DEEP_STUBS);
        when(dockerClient.listContainersCmd().exec()).thenReturn(List.of());

        try (MockedStatic<DockerClients> dockerClients = mockStatic(DockerClients.class))
        {
            dockerClients.when(DockerClients::build).thenReturn(dockerClient);

            new TestContainer("test-service", new MockEnvironment(), properties());
        }
    }

    private static class TestContainer extends AbstractBuildingEmbeddedContainer<TestProperties>
    {
        TestContainer(String service, org.springframework.core.env.ConfigurableEnvironment environment, TestProperties properties)
        {
            super(service, environment, properties);
        }

        @Override
        protected boolean isContainerReady(TestProperties properties)
        {
            return true;
        }
    }

    private static class TestProperties extends AbstractEmbeddedContainerProperties
    {
    }
}
