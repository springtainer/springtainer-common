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
     * Disabled ({@code 0}) by default: age alone can't distinguish an abandoned container from one still in active use by a single long-running test
     * suite, so a blind periodic check risks force-removing containers that just haven't needed a new context in a while. Set to a positive value only if
     * every consumer's test suites are known to finish comfortably within {@link #afterMinutes}.
     */
    private int checkIntervalSeconds = 0;
}
