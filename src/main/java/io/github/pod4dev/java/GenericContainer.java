package io.github.pod4dev.java;

import io.github.pod4dev.java.exceptions.PodmanException;


public interface GenericContainer extends AutoCloseable {

    /**
     * Creates the pod or container and immediately starts it. All created resources will be cleared
     * out when a SIGTERM is received or pods exit.
     */
    void start() throws PodmanException;

    /**
     * Stops the pod or container with clearing created volumes.
     */
    void stop() throws PodmanException;

    /**
     * Specify service and its port for expose.
     *
     * @param serviceName name of service to expose.
     * @param exposedPort port to expose.
     * @return container with exposed services.
     */
    GenericContainer withExposedService(final String serviceName,
                                        final int exposedPort) throws PodmanException;

    /**
     * Do resources cleanup after stopping.
     *
     * @param doCleanup default if true.
     * @return customised container.
     */
    GenericContainer withCleanup(boolean doCleanup);

    /**
     * Getting mapped host for the given service's name and exposed port.
     *
     * @param serviceName the service name.
     * @param exposedPort the exposed port.
     * @return mapped host.
     */
    String getMappedHost(final String serviceName, final int exposedPort) throws PodmanException;

    /**
     * Getting mapped port for the given service's name and exposed port.
     *
     * @param serviceName the service name.
     * @param exposedPort the exposed port.
     * @return mapped host.
     */
    int getMappedPort(final String serviceName, final int exposedPort) throws PodmanException;

    @Override
    default void close() throws Exception {
        this.stop();
    }
}
