package io.github.pod4dev.java;

import lombok.Getter;

@Getter
public class PodmanImage {

    private final String registry;
    private final String name;
    private final String tag;

    private PodmanImage(String registry, String name, String tag) {
        if (name == null) {
            throw new NullPointerException("name is null");
        }
        this.registry = registry;
        this.name = name;
        this.tag = tag;
    }

    public static PodmanImage of(String registry, String name, String tag) {
        return new PodmanImage(registry, name, tag);
    }

    public static PodmanImage of(String name, String tag) {
        return of(Constants.DEFAULT_REGISTRY, name, tag);
    }

    public static PodmanImage of(String name) {
        return of(name, null);
    }

    public String build() {
        StringBuilder image = new StringBuilder();
        if (registry != null) {
            image.append(registry);
            image.append("/");
        }
        image.append(name);
        if (tag != null) {
            image.append(":");
            image.append(tag);
        }
        return image.toString();
    }
}
