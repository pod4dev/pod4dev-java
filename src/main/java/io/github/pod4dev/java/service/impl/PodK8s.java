package io.github.pod4dev.java.service.impl;

import io.github.pod4dev.java.core.ServiceBinding;
import io.github.pod4dev.java.core.Utils;
import io.github.pod4dev.java.exceptions.PodmanException;
import io.github.pod4dev.java.podman.PodmanClient;
import io.github.pod4dev.libpodj.ApiException;
import io.github.pod4dev.libpodj.api.PodsApi;
import io.github.pod4dev.libpodj.model.InspectHostPort;
import io.github.pod4dev.libpodj.model.InspectPodData;
import io.github.pod4dev.libpodj.model.InspectPodInfraConfig;
import io.github.pod4dev.libpodj.model.PlayKubeReport;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;


/**
 * Work with /kube/play API.
 */
@Slf4j
public class PodK8s implements io.github.pod4dev.java.service.PodK8s {

    private final PodmanClient client;
    private final String yamlPath;

    private final List<ServiceBinding> servicesBindings = new ArrayList<>();

    private boolean isRunning;
    private boolean doCleanup = true;
    private boolean doRemoveVolumes = true;

    /**
     * Creates player with specified paths for socket file and k8s YAML specification.
     *
     * @param podmanUri host address.
     * @param yamlPath  path to k8s YAML specification.
     * @throws PodmanException if {@link #yamlPath} is empty or error during initialization is happened.
     */
    private PodK8s(URI podmanUri, String yamlPath) throws PodmanException {
        this.yamlPath = yamlPath;
        this.client = new PodmanClient(podmanUri);
    }

    /**
     * Creates player with specified path for k8s YAML specification. The socket path is autodetected via {@code PODMAN_HOST} environment
     * variable.
     *
     * @param yamlPath path to k8s YAML specification.
     * @throws PodmanException if {@link #yamlPath} is empty or error during initialization is happened.
     */
    public PodK8s(String yamlPath) throws PodmanException {
        this(Utils.getPodmanUri(), yamlPath);
    }

    @Override
    public PodmanClient getClient() {
        return this.client;
    }

    @Override
    public boolean isRunning() {
        return this.isRunning;
    }

    @Override
    public PodK8s withExposedService(final String serviceName, final Integer exposedPort) throws PodmanException {

        final Predicate<ServiceBinding> isBindingExist = serviceBinding -> Objects.equals(serviceBinding.getServiceName(), serviceName)
                && Objects.equals(serviceBinding.getExposedPort(), exposedPort);

        if (!servicesBindings.isEmpty() && servicesBindings.stream().anyMatch(isBindingExist)) {
            throw new PodmanException("Binging[serviceName=%s, exposedPort=%d] already exists".formatted(serviceName, exposedPort));
        }

        servicesBindings.add(new ServiceBinding(serviceName, this.getMappedHost(), exposedPort));

        return this;
    }

    @Override
    public PodK8s withCleanup(boolean doCleanup) {
        this.doCleanup = doCleanup;
        return this;
    }

    @Override
    public PodK8s withRemoveVolumes(boolean doRemoveVolumes) {
        this.doRemoveVolumes = doRemoveVolumes;
        return this;
    }

    @Override
    public void start() throws PodmanException {
        Runtime.getRuntime().addShutdownHook(new Thread(this::stop));

        final var podsApi = new PodsApi(this.client);
        String yaml = Utils.readYaml(this.yamlPath);

        PlayKubeReport report;
        try {
            List<String> ports = servicesBindings.stream()
                    .map(serviceBinding -> "%d".formatted(serviceBinding.getExposedPort()))
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

        report.getPods().forEach(pod -> {
            InspectPodData inspectation;
            try {
                inspectation = podsApi.podInspectLibpod(pod.getID()).execute();
            } catch (ApiException e) {
                throw new PodmanException(e);
            }
            Map<Integer, Integer> mappings = Optional.ofNullable(inspectation.getInfraConfig())
                    .map(InspectPodInfraConfig::getPortBindings)
                    .stream()
                    .map(Map::entrySet)
                    .flatMap(Collection::stream)
                    .filter(entry -> entry.getValue().stream().anyMatch(inspectHostPort -> "0.0.0.0".equals(inspectHostPort.getHostIp())))
                    .collect(Collectors.toMap(
                            entry -> Integer.valueOf(entry.getKey().split("/")[0]),
                            entry -> Integer.valueOf(entry.getValue().stream()
                                    .filter(inspectHostPort -> "0.0.0.0".equals(inspectHostPort.getHostIp()))
                                    .findFirst()
                                    .map(InspectHostPort::getHostPort)
                                    .get())
                    ));
            for (var sb : servicesBindings) {
                if (sb.getServiceName().equals(inspectation.getName())) {
                    sb.setMappedPort(mappings.get(sb.getExposedPort()));
                }
            }
        });

        this.isRunning = true;
    }

    @Override
    public void stop() throws PodmanException {
        final var pods = new PodsApi(this.client);

        String yaml = Utils.readYaml(this.yamlPath);

        try {
            pods.playKubeDownLibpod_0()
                    .force(this.doRemoveVolumes)
                    .request(yaml)
                    .execute();
        } catch (ApiException e) {
            throw new PodmanException(e);
        }

        this.isRunning = false;
    }

    @Override
    public Integer getMappedPort(String serviceName, Integer exposedPort) throws PodmanException {
        if (!this.isRunning) {
            throw new PodmanException("Is not running");
        }

        final Predicate<ServiceBinding> isBindingExist = serviceBinding -> Objects.equals(serviceBinding.getServiceName(), serviceName)
                && Objects.equals(serviceBinding.getExposedPort(), exposedPort)
                && Objects.nonNull(serviceBinding.getMappedPort());
        return this.servicesBindings
                .stream()
                .filter(isBindingExist)
                .map(ServiceBinding::getMappedPort)
                .findAny()
                .orElseThrow(() -> new PodmanException("Here is no mapped port"));
    }
}
