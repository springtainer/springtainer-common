package com.avides.springboot.testcontainer.common;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.easymock.PowerMock;
import org.powermock.api.easymock.annotation.MockStrict;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InfoCmd;
import com.github.dockerjava.api.model.Info;
import com.github.dockerjava.core.DockerClientBuilder;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ DockerClientBuilder.class })
public class TestcontainerContextTest
{
    @MockStrict
    private DockerClientBuilder dockerClientBuilder;

    @MockStrict
    private DockerClient dockerClient;

    @MockStrict
    private InfoCmd infoCmd;

    @MockStrict
    private Info info;

    @Before
    public void before()
    {
        PowerMock.mockStatic(DockerClientBuilder.class);
    }

    @Test
    public void testCreateDockerClient()
    {
        Whitebox.setInternalState(TestcontainerContext.class, "");

        DockerClientBuilder.getInstance();
        PowerMock.expectLastCall().andReturn(dockerClientBuilder);

        dockerClientBuilder.build();
        PowerMock.expectLastCall().andReturn(dockerClient);

        dockerClient.infoCmd();
        PowerMock.expectLastCall().andReturn(infoCmd);

        infoCmd.exec();
        PowerMock.expectLastCall().andReturn(info);

        info.getOsType();
        PowerMock.expectLastCall().andReturn("linux");

        PowerMock.replayAll();
        DockerClient dockerClient = TestcontainerContext.createDockerClient();
        PowerMock.verifyAll();

        assertNotNull(dockerClient);
        assertTrue(TestcontainerContext.isRunningOnLinux());
    }

    @Test
    public void testCreateDockerClientWithSecondCall()
    {
        Whitebox.setInternalState(TestcontainerContext.class, "windows");

        DockerClientBuilder.getInstance();
        PowerMock.expectLastCall().andReturn(dockerClientBuilder);

        dockerClientBuilder.build();
        PowerMock.expectLastCall().andReturn(dockerClient);

        PowerMock.replayAll();
        DockerClient dockerClient = TestcontainerContext.createDockerClient();
        PowerMock.verifyAll();

        assertNotNull(dockerClient);
        assertFalse(TestcontainerContext.isRunningOnLinux());
    }

    @Test
    public void testTestIsRunningOnLinuxWithLinux()
    {
        Whitebox.setInternalState(TestcontainerContext.class, "linux");
        assertTrue(TestcontainerContext.isRunningOnLinux());
    }

    @Test
    public void testTestIsRunningOnLinuxWithMac()
    {
        Whitebox.setInternalState(TestcontainerContext.class, "mac");
        assertFalse(TestcontainerContext.isRunningOnLinux());
    }

    @Test
    public void testTestIsRunningOnLinuxWithWindows()
    {
        Whitebox.setInternalState(TestcontainerContext.class, "windows");
        assertFalse(TestcontainerContext.isRunningOnLinux());
    }
}
