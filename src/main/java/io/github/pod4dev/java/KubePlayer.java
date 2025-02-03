package io.github.pod4dev.java;

import io.github.pod4dev.java.exceptions.PodmanException;
import io.github.pod4dev.libpodj.ApiClient;
import io.github.pod4dev.libpodj.ApiException;
import io.github.pod4dev.libpodj.api.PodsApi;
import io.github.pod4dev.libpodj.model.PlayKubeReport;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.unixdomainsockets.UnixDomainSocketFactory;

import java.io.File;
import java.io.IOException;
import java.net.URI;
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
     * Creates player with specified paths for socket file and k8s YAML specification.
     *
     * @param podmanUri host address.
     * @param yamlPath  path to k8s YAML specification.
     * @throws PodmanException if {@link #yamlPath} is empty or error during initialization is happened.
     */
    private KubePlayer(URI podmanUri, String yamlPath) throws PodmanException {
        var httpClientBuilder = new OkHttpClient.Builder();
        this.api = new FixedApiClient();
        this.hostname = Utils.getHost(podmanUri);

        switch (podmanUri.getScheme().toLowerCase()) {
            case "unix" -> {
                httpClientBuilder.socketFactory(new UnixDomainSocketFactory(new File(podmanUri.getPath())));
                this.api.setBasePath("http://localhost/v5.0.0");
            }
            case "http", "https", "tcp" -> {
                this.api.setBasePath(podmanUri.toString());
            }
            default -> throw new PodmanException("Wrong schema");
        }
        this.api.setHttpClient(httpClientBuilder.build());

        this.yamlPath = yamlPath;
    }

    /**
     * Creates player with specified path for k8s YAML specification. The socket path is autodetected via {@code PODMAN_HOST} environment
     * variable.
     *
     * @param yamlPath path to k8s YAML specification.
     * @throws PodmanException if {@link #yamlPath} is empty or error during initialization is happened.
     */
    public KubePlayer(String yamlPath) throws PodmanException {
        this(Utils.getPodmanUri(), yamlPath);
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
