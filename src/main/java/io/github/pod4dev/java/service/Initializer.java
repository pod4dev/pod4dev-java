package io.github.pod4dev.java.service;

import io.github.pod4dev.java.exceptions.PodmanException;
import io.github.pod4dev.java.service.impl.PlayerImpl;
import lombok.experimental.UtilityClass;

import java.net.URI;


@UtilityClass
public class Initializer {

    /**
     * Creates player with specified paths for socket file and k8s YAML specification.
     *
     * @param podmanUri host address.
     * @param yamlPath path to k8s YAML specification.
     * 
     * @throws PodmanException if {@link #yamlPath} is empty or error during initialization is happened.
     */
    public static Player createPodK8s(URI podmanUri, String yamlPath) throws PodmanException {
        return new PlayerImpl(podmanUri, yamlPath);
    }

    /**
     * Creates player with specified path for k8s YAML specification. The socket path is autodetected via {@code PODMAN_HOST} environment
     * variable.
     *
     * @param yamlPath path to k8s YAML specification.
     * 
     * @throws PodmanException if {@link #yamlPath} is empty or error during initialization is happened.
     */
    public static Player createPodK8s(String yamlPath) throws PodmanException {
        return new PlayerImpl(yamlPath);
    }
}
