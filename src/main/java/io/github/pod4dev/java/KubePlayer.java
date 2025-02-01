package io.github.pod4dev.java;

import io.github.pod4dev.java.exceptions.PodmanException;
import io.github.pod4dev.libpodj.ApiClient;
import io.github.pod4dev.libpodj.ApiException;
import io.github.pod4dev.libpodj.api.PodsApi;
import io.github.pod4dev.libpodj.api.SystemApi;
import io.github.pod4dev.libpodj.model.LibpodInfo;
import io.github.pod4dev.libpodj.model.PlayKubeReport;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.unixdomainsockets.UnixDomainSocketFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;


/**
 * Work with /kube/play API.
 */
@Slf4j
public class KubePlayer implements GenericContainer {

    private final ApiClient api;
    private final String yamlPath;
    private final String hostname;

    private final List<ServiceBinding> servicesBindings = new ArrayList<>();

    private boolean doCleanup = true;
    private boolean doRemoveVolumes = true;

    /**
     * Creates player with specified path for k8s YAML specification. The socket path is autodetected via {@code PODMAN_SOCKET} environment
     * variable.
     *
     * @param yamlPath path to k8s YAML specification.
     * @throws PodmanException if {@link #yamlPath} is empty or error during initialization is happened.
     */
    public KubePlayer(String yamlPath) throws PodmanException {
        this(System.getenv(Constants.ENV_PODMAN_SOCKET), yamlPath);
    }

    /**
     * Creates player with specified paths for socket file and k8s YAML specification. The socket path is autodetected via
     * {@code PODMAN_SOCKET} environment variable.
     *
     * @param socketPath path to socket file.
     * @param yamlPath   path to k8s YAML specification.
     * @throws PodmanException if {@link #yamlPath} is empty or error during initialization is happened.
     */
    public KubePlayer(String socketPath, String yamlPath) throws PodmanException {
        if (socketPath == null || socketPath.isEmpty()) {
            throw new PodmanException("Environment variable " + Constants.ENV_PODMAN_SOCKET + " is not set");
        }

        var httpClient = new OkHttpClient.Builder()
                .socketFactory(new UnixDomainSocketFactory(new File(socketPath)))
                .build();
        this.api = new FixedApiClient()
                .setHttpClient(httpClient)
                .setBasePath("http://localhost/v5.0.0");

        this.yamlPath = yamlPath;

        final SystemApi systemApi = new SystemApi(this.api);

        LibpodInfo libpodInfo = null;
        try {
            libpodInfo = systemApi.systemInfoLibpod().execute();
        } catch (ApiException ex) {
            throw new PodmanException("Doesn't initialized", ex);
        }
        this.hostname = libpodInfo.getHost().getHostname();
    }

    @Override
    public KubePlayer withExposedService(String serviceName,
                                         int exposedPort) throws PodmanException {
        final Predicate<ServiceBinding> isBindingExist = serviceBinding -> Objects.equals(serviceBinding.getServiceName(), serviceName)
                && Objects.equals(serviceBinding.getExposedPort(), exposedPort);

        if (!servicesBindings.isEmpty() && servicesBindings.stream().anyMatch(isBindingExist)) {
            throw new PodmanException("Binging[serviceName=%s, exposedPort=%d] already exists".formatted(serviceName, exposedPort));
        }

        final int mappedPort = Utils.findFreePort(servicesBindings.stream().map(ServiceBinding::getMappedPort).toList());

        servicesBindings.add(new ServiceBinding(serviceName, this.hostname, exposedPort, mappedPort));
        return this;
    }

    @Override
    public KubePlayer withCleanup(boolean doCleanup) {
        this.doCleanup = doCleanup;
        return this;
    }

    @Override
    public KubePlayer withRemoveVolumes(boolean doRemoveVolumes) {
        this.doRemoveVolumes = doRemoveVolumes;
        return this;
    }

    @Override
    public void start() throws PodmanException {
        Runtime.getRuntime().addShutdownHook(new Thread(this::stop));

        final var podsApi = new PodsApi(this.api);

        String yaml = null;
        try {
            yaml = Utils.readYaml(this.yamlPath);
        } catch (IOException e) {
            throw new PodmanException(e);
        }

        PlayKubeReport report = null;
        try {
            List<String> ports = servicesBindings.stream()
                    .map(serviceBinding -> "%d:%d".formatted(
                            serviceBinding.getMappedPort(),
                            serviceBinding.getExposedPort()
                    ))
                    .toList();
            report = podsApi.playKubeLibpod_0()
                    .publishPorts(ports)
                    .wait(this.doCleanup)
                    .start(true)
                    .request(yaml)
                    .execute();
        } catch (ApiException e) {
            throw new PodmanException(e);
        }

        if (report == null || report.getPods() == null || report.getPods().isEmpty()) {
            throw new PodmanException("There is no related pods");
        }
    }

    @Override
    public void stop() throws PodmanException {
        final var pods = new PodsApi(this.api);

        String yaml = null;
        try {
            yaml = Utils.readYaml(this.yamlPath);
        } catch (IOException e) {
            throw new PodmanException(e);
        }

        try {
            pods.playKubeDownLibpod_0()
                    .force(this.doRemoveVolumes)
                    .request(yaml)
                    .execute();
        } catch (ApiException e) {
            throw new PodmanException(e);
        }
    }

    @Override
    public String getMappedHost(String serviceName, int exposedPort) {
        final Predicate<ServiceBinding> isBindingExist = serviceBinding -> Objects.equals(serviceBinding.getServiceName(), serviceName)
                && Objects.equals(serviceBinding.getExposedPort(), exposedPort);
        return this.servicesBindings
                .stream()
                .filter(isBindingExist)
                .map(ServiceBinding::getMappedHost)
                .findAny()
                .orElse(null);
    }

    @Override
    public int getMappedPort(String serviceName, int exposedPort) throws PodmanException {
        final Predicate<ServiceBinding> isBindingExist = serviceBinding -> Objects.equals(serviceBinding.getServiceName(), serviceName)
                && Objects.equals(serviceBinding.getExposedPort(), exposedPort);
        return this.servicesBindings
                .stream()
                .filter(isBindingExist)
                .map(ServiceBinding::getMappedPort)
                .findAny()
                .orElseThrow(() -> new PodmanException("Here is no mapped port"));
    }
}
