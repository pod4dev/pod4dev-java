package io.github.pod4dev.java;

import io.github.pod4dev.java.service.impl.PodK8s;
import io.github.pod4dev.libpodj.ApiException;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Paths;

@Slf4j
class PodK8sTest {

    protected static final PodK8s ENVIRONMENT = new PodK8s(Paths.get("src/test/resources/test.yaml").toAbsolutePath().toString())
            .withExposedService("pod4dev-java-1", 80)
            .withExposedService("pod4dev-java-1", 81)
            .withExposedService("pod4dev-java-2", 2000);

    static {
        ENVIRONMENT.start();
    }

    @Test
    void startStop() throws IOException, ApiException {
        /*------ Arranges ------*/
        var client = new OkHttpClient.Builder().build();

        /*------ Actions ------*/

        var mappedPort1 = ENVIRONMENT.getMappedPort("pod4dev-java-1", 80);
        var mappedHost1 = ENVIRONMENT.getMappedHost();
        var mappedPort2 = ENVIRONMENT.getMappedPort("pod4dev-java-1", 81);
        var mappedHost2 = ENVIRONMENT.getMappedHost();
        var mappedPort3 = ENVIRONMENT.getMappedPort("pod4dev-java-2", 2000);
        var mappedHost3 = ENVIRONMENT.getMappedHost();

        String logTemplate = "Mapped: %s:%d";
        log.info(logTemplate.formatted(mappedHost1, mappedPort1));
        log.info(logTemplate.formatted(mappedHost2, mappedPort2));
        log.info(logTemplate.formatted(mappedHost3, mappedPort3));

        var result1 = client
                .newCall(new Request.Builder().url("http://%s:%d".formatted(mappedHost1, mappedPort1)).build())
                .execute();
        var result2 = client
                .newCall(new Request.Builder().url("http://%s:%d".formatted(mappedHost2, mappedPort2)).build())
                .execute();
        var result3 = client
                .newCall(new Request.Builder().url("http://%s:%d".formatted(mappedHost3, mappedPort3)).build())
                .execute();

        /*------ Asserts ------*/
        Assertions.assertNotEquals(80, mappedPort1);
        Assertions.assertNotEquals(81, mappedPort2);
        Assertions.assertNotEquals(2000, mappedPort3);
        Assertions.assertTrue(result1.isSuccessful());
        Assertions.assertTrue(result2.isSuccessful());
        Assertions.assertTrue(result3.isSuccessful());
    }
}
