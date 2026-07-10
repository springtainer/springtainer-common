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
     * Disabled ({@code 0}) by default: a blind periodic re-check has no way to tell "abandoned" apart from "still in active use by a single, long-running
     * test suite that just hasn't needed a new context/container in a while" - it only knows a container's age. This was observed in practice against a
     * ~30 minute single-context IT suite: every one of its containers got force-removed by this periodic check almost exactly at the {@link #afterMinutes}
     * mark while the suite was still legitimately using them, since age alone crossed the threshold. With this disabled, the check only runs at the moment
     * a new {@code EmbeddedContainerCleanup} bean is created (i.e. a genuinely new Spring context is built) - tied to real activity rather than a fixed
     * clock, so it can still catch a {@link #maxConcurrentPerIssuer} breach or truly stale containers whenever further contexts get created, without ever
     * acting on a container purely because a timer fired.
     * <p>
     * Set to a positive value to opt back into the background schedule; only do so if every consumer's test suites are known to finish comfortably within
     * {@link #afterMinutes}.
     */
    private int checkIntervalSeconds = 0;
}
