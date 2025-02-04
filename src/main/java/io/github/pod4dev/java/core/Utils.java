package io.github.pod4dev.java.core;

import io.github.pod4dev.java.exceptions.PodmanException;
import lombok.experimental.UtilityClass;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Random;

@UtilityClass
public final class Utils {

    public static String readYaml(String yamlPath) throws PodmanException {
        try {
            List<Path> files = Files.isDirectory(Path.of(yamlPath))
                    ? Files.list(Path.of(yamlPath)).toList()
                    : List.of(Path.of(yamlPath));

            StringBuilder resultStringBuilder = new StringBuilder();
            for (Path file : files) {
                if (file.getFileName().toString().endsWith(".yaml") || file.getFileName().toString().endsWith(".yml")) {
                    try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file.toFile())))) {
                        String line;
                        while ((line = br.readLine()) != null) {
                            resultStringBuilder.append(line).append(System.lineSeparator());
                        }
                        resultStringBuilder.append("---").append(System.lineSeparator());
                    }
                }
            }

            return resultStringBuilder.toString();
        } catch (IOException ex) {
            throw new PodmanException(ex);
        }
    }

    public static int findFreePort(List<Integer> binded) throws PodmanException {
        Integer result = null;
        final Random randomizer = new Random();
        final int min = 30000;
        final int max = 50000;
        int counter = max - min;
        while (result == null && counter > 0) {
            int port = randomizer.nextInt(min, max);
            if (binded.contains(port)) {
                counter--;
                continue;
            }
            try (ServerSocket serverSocket = new ServerSocket(port)) {
                if (serverSocket.getLocalPort() == port) {
                    result = port;
                    break;
                }
            } catch (IOException ignored) {
                counter--;
                continue;
            }
        }
        if (result == null) {
            throw new PodmanException("There is no free port");
        }
        return result;
    }

    public static URI getPodmanUri() {
        String podmanUri = System.getenv(Constants.ENV_PODMAN_HOST);
        if (podmanUri == null) {
            podmanUri = System.getenv(Constants.ENV_DOCKER_HOST);
        }
        if (podmanUri == null) {
            throw new PodmanException("No environment variable defined");
        }
        return URI.create(podmanUri);
    }

    public static String getUnixDomainSocket(URI podmanUri) {
        return "unix".toLowerCase().equals(podmanUri.getScheme()) ? podmanUri.getPath() : null;
    }

    public static String getHost(URI podmanUri) {
        return switch (podmanUri.getScheme().toLowerCase()) {
            case "unix" -> "localhost";
            case "http", "https", "tcp" -> podmanUri.getHost();
            default -> null;
        };
    }
}
