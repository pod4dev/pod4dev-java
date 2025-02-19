package io.github.pod4dev.java.service;

import io.github.pod4dev.java.exceptions.PodmanException;


public interface PodK8s extends GenericService {

    /**
     * Specify service and its port for expose.
     *
     * @param serviceName name of service to expose.
     * @param exposedPort port to expose.
     * @return container with exposed services.
     */
    PodK8s withExposedService(final String serviceName,
                              final Integer exposedPort) throws PodmanException;


    /**
     * Getting mapped port for the given service's name and exposed port.
     *
     * @param serviceName the service name.
     * @param exposedPort the exposed port.
     * @return mapped host.
     */
    Integer getMappedPort(final String serviceName, final Integer exposedPort) throws PodmanException;
}
