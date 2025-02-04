package io.github.pod4dev.java.core;

import io.github.pod4dev.java.exceptions.PodmanException;
import io.github.pod4dev.java.podman.PodmanClient;
import io.github.pod4dev.libpodj.ApiException;
import io.github.pod4dev.libpodj.api.SystemApi;
import io.github.pod4dev.libpodj.model.HostInfo;
import io.github.pod4dev.libpodj.model.LibpodInfo;

import java.util.Optional;


public interface GenericContainer extends AutoCloseable {

    /**
     * Check if service is running.
     *
     * @return check result.
     */
    boolean isRunning();

    /**
     * Get API client.
     *
     * @return API client.
     */
    PodmanClient getClient();

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
                                        final Integer exposedPort) throws PodmanException;

    /**
     * Do resources cleanup after stopping.
     *
     * @param doCleanup default is true.
     * @return customised container.
     */
    GenericContainer withCleanup(boolean doCleanup);

    /**
     * Do volumes cleanup after stopping.
     *
     * @param doRemoveVolumes default is true.
     * @return customised container.
     */
    GenericContainer withRemoveVolumes(boolean doRemoveVolumes);

    /**
     * Getting mapped host
     *
     * @return mapped host.
     */
    default String getMappedHost() throws PodmanException {
        final SystemApi systemApi = new SystemApi(this.getClient());

        LibpodInfo libpodInfo = null;
        try {
            libpodInfo = systemApi.systemInfoLibpod().execute();
        } catch (ApiException e) {
            throw new PodmanException(e);
        }

        return Optional.ofNullable(libpodInfo.getHost()).map(HostInfo::getHostname).orElse(null);
    }

    /**
     * Getting mapped port for the given service's name and exposed port.
     *
     * @param serviceName the service name.
     * @param exposedPort the exposed port.
     * @return mapped host.
     */
    Integer getMappedPort(final String serviceName, final Integer exposedPort) throws PodmanException;

    @Override
    default void close() throws Exception {
        this.stop();
    }
}
