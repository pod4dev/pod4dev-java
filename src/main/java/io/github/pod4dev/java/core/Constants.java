package io.github.pod4dev.java.core;

import lombok.experimental.UtilityClass;

@UtilityClass
public final class Constants {

    public static final String ENV_PODMAN_HOST = "PODMAN_HOST";
    public static final String ENV_DOCKER_HOST = "DOCKER_HOST";
    public static final String DEFAULT_REGISTRY = "docker.io";
    public static final String IMAGE_SOCAT = "alpine/socat";
}
