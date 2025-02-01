package io.github.pod4dev.java;

import lombok.extern.slf4j.Slf4j;


@Slf4j
public class CleaningGenericContainerResourcesAction<T extends GenericContainer> implements Runnable {

    private final T delegate;

    public CleaningGenericContainerResourcesAction(T delegate) {
        this.delegate = delegate;
    }

    @Override
    public void run() {
        try {
            log.info("Start cleaning...");
            delegate.stop();
            log.info("Cleaned.");
        } catch (Exception ex) {
            log.error("Error cleaning", ex);
        }
    }
}
