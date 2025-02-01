package io.github.pod4dev.java;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Paths;


class KubePlayerTest {

    protected static final KubePlayer ENVIRONMENT = new KubePlayer(
        "/var/run/user/1000/podman/podman.sock",
        Paths.get("src/test/resources/").toAbsolutePath().toString()
    );

    protected static final String SERVICE = "nginx";
    protected static final int PORT = 80;

    static {
        ENVIRONMENT.withExposedService(SERVICE, PORT).start();
    }

    @Test
    void startStop() throws IOException {
        /*------ Arranges ------*/
        var client = new OkHttpClient.Builder().build();

        /*------ Actions ------*/

        var mappedPort = ENVIRONMENT.getMappedPort(SERVICE, PORT);
        var result = client
            .newCall(new Request.Builder().url("http://localhost:%d".formatted(mappedPort)).build())
            .execute();

        /*------ Asserts ------*/
        Assertions.assertNotEquals(PORT, mappedPort);
        Assertions.assertTrue(result.isSuccessful());
    }
}
