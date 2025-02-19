package io.github.pod4dev.java.service;

import io.github.pod4dev.java.core.Utils;
import io.github.pod4dev.java.exceptions.PodmanException;
import io.github.pod4dev.java.podman.PodmanClient;

public interface GenericService extends AutoCloseable {

    /**
     * Check if service is running.
     *
     * @return check result.
     */
    boolean isRunning();

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
     * Do resources cleanup after stopping.
     *
     * @param doCleanup default is true.
     * @return customised container.
     */
    PodK8s withCleanup(boolean doCleanup);

    /**
     * Do volumes cleanup after stopping.
     *
     * @param doRemoveVolumes default is true.
     * @return customised container.
     */
    PodK8s withRemoveVolumes(boolean doRemoveVolumes);

    /**
     * Getting mapped host
     *
     * @return mapped host.
     */
    default String getMappedHost() throws PodmanException {
        return Utils.getHost(getClient().podmanURI());
    }

    @Override
    default void close() throws Exception {
        this.stop();
    }

    /**
     * Get API client.
     *
     * @return API client.
     */
    PodmanClient getClient();
}
