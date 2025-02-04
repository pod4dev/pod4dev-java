package io.github.pod4dev.java.core;

import lombok.Getter;
import lombok.Setter;

/**
 * Service binging.
 */
@Getter
public class ServiceBinding {

    /**
     * Name of service that owns the exposed port.
     */
    private final String serviceName;

    /**
     * Host of exposed service.
     */
    private final String mappedHost;

    /**
     * Exposed port.
     */
    private final Integer exposedPort;

    /**
     * Mapped port.
     */
    @Setter
    private Integer mappedPort;

    /**
     * Creates port binding.
     *
     * @param serviceName the name of service that owns by the given port.
     * @param mappedHost  the service's host.
     * @param exposedPort the exposed port.
     * @param mappedPort  the mapped port.
     */
    public ServiceBinding(String serviceName,
                          String mappedHost,
                          Integer exposedPort,
                          Integer mappedPort) {
        this.serviceName = serviceName;
        this.mappedHost = mappedHost;
        this.exposedPort = exposedPort;
        this.mappedPort = mappedPort;
    }

    public ServiceBinding(String serviceName, String mappedHost, Integer exposedPort) {
        this(serviceName, mappedHost, exposedPort, null);
    }
}
