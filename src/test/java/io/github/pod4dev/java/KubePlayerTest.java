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
            Paths.get("src/test/resources/test.yaml").toAbsolutePath().toString()
    );

    static {
        ENVIRONMENT
                .withExposedService("test-1", 80)
                .withExposedService("test-2", 81)
                .start();
    }

    @Test
    void startStop() throws IOException {
        /*------ Arranges ------*/
        var client = new OkHttpClient.Builder().build();

        /*------ Actions ------*/

        var mappedPort1 = ENVIRONMENT.getMappedPort("test-1", 80);
        var mappedHost1 = ENVIRONMENT.getMappedHost("test-1", 80);
        var mappedPort2 = ENVIRONMENT.getMappedPort("test-2", 81);
        var mappedHost2 = ENVIRONMENT.getMappedHost("test-2", 81);

        var result1 = client
                .newCall(new Request.Builder().url("http://%s:%d".formatted(mappedHost1, mappedPort1)).build())
                .execute();
        var result2 = client
                .newCall(new Request.Builder().url("http://%s:%d".formatted(mappedHost2, mappedPort2)).build())
                .execute();

        /*------ Asserts ------*/
        Assertions.assertNotEquals(80, mappedPort1);
        Assertions.assertNotEquals(81, mappedPort2);
        Assertions.assertTrue(result1.isSuccessful());
        Assertions.assertTrue(result2.isSuccessful());
    }
}
