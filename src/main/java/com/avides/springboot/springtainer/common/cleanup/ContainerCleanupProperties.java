package com.avides.springboot.springtainer.common.cleanup;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@ConfigurationProperties("embedded.container.cleanup")
@Getter
@Setter
@ToString
public class ContainerCleanupProperties
{
    private boolean enabled = true;

    private int afterMinutes = 10;

    private int maxConcurrentPerIssuer = 10;

    /**
     * Interval (in seconds) at which the stale-container check re-runs in the background, independent of new container/context creation.
     * <p>
     * Without this, the check only runs at the moment a new {@code EmbeddedContainerCleanup} bean is created (i.e. a genuinely new Spring context is built).
     * Once enough distinct test contexts are cached, later tests reuse them without creating any new container, so the check simply stops being invoked and a
     * {@link #maxConcurrentPerIssuer} breach that only manifests after the last new context was created is never caught.
     * <p>
     * Set to {@code 0} to disable the background schedule and fall back to the old check-on-creation-only behavior.
     */
    private int checkIntervalSeconds = 60;
}
