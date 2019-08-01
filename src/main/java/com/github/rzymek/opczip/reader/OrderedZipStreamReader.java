package com.github.rzymek.opczip.reader;

import java.io.*;
import java.util.*;
import java.util.logging.Logger;

import static java.util.stream.Collectors.toSet;

abstract class OrderedZipStreamReader {
    static final Logger log = Logger.getLogger(OrderedZipStreamReader.class.getName());

    private Map<String, ConsumerEntry> consumers = new HashMap<>();

    private static class ConsumerEntry {
        Consumer processor;
        Set<String> dependencies;
        boolean consumed = false;

        ConsumerEntry(Consumer consumer, String... dependencies) {
            this.processor = consumer;
            this.dependencies = Set.of(dependencies);
        }
    }

    public OrderedZipStreamReader with(Consumer consumer, String entry, String... dependencies) {
        ConsumerEntry prev = consumers.put(entry, new ConsumerEntry(consumer, dependencies));
        if (prev != null) {
            throw new IllegalStateException(entry + " already registered");
        }
        return this;
    }

    public void read(InputStream in) throws IOException {
        validateDependencies();
        try (SkippableZipInputStream zip = open(in)) {
            while (hasPendingConsumers()) {
                String name = zip.getNextEntry();
                log.info(name);
                if (name == null) {
                    break;
                }
                ConsumerEntry consumer = consumers.get(name);
                if (consumer == null) {
                    zip.skipEntry();
                } else {
                    if (isEveryConsumed(consumer.dependencies)) {
                        process(zip.getUncompressedInputStream(), consumer);
                        consumers.entrySet().stream()
                                .filter(e -> !e.getValue().consumed)
                                .filter(e -> e.getValue().dependencies.contains(name))
                                .filter(e -> isEveryConsumedBut(e.getValue().dependencies, name))
                                .forEach(e -> process(zip.uncompressedTransferred(getTempInputStream(e.getKey())), e.getValue()));
                    } else {
                        zip.transferCompressedTo(getTempOutputStream(name));
                    }
                }
                zip.closeEntry();
            }
        }
    }

    private void validateDependencies() {
        consumers.forEach((name, consumer) -> {
            Set<String> notProcessedDeps = consumer.dependencies.stream()
                    .filter(dep -> !consumers.containsKey(dep))
                    .collect(toSet());
            if (notProcessedDeps.isEmpty()) {
                return;
            }
            throw new IllegalStateException(name + " has a dependencies that are not registered for processing: " + notProcessedDeps);
        });
    }

    private boolean isEveryConsumedBut(Set<String> dependencies, String name) {
        return !dependencies.stream()
                .filter(dep -> !dep.equals(name))
                .anyMatch(this::isUnconsumedDependency);
    }


    protected void process(InputStream in, ConsumerEntry entry) throws UncheckedIOException {
        try {
            entry.processor.accept(in);
            entry.consumed = true;
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }

    }

    private boolean isEveryConsumed(Set<String> dependencies) {
        return !dependencies.stream().anyMatch(this::isUnconsumedDependency);
    }

    protected abstract OutputStream getTempOutputStream(String name) throws IOException;

    protected abstract InputStream getTempInputStream(String name) throws UncheckedIOException;

    protected abstract SkippableZipInputStream open(InputStream in);

    private boolean isUnconsumedDependency(String name) {
        return consumers.values().stream()
                .filter(e -> !e.consumed)
                .anyMatch(e -> e.dependencies.contains(name));
    }

    private boolean hasPendingConsumers() {
        return consumers.values().stream().anyMatch(e -> !e.consumed);
    }


    @FunctionalInterface
    public interface Consumer {
        void accept(InputStream in) throws IOException;
    }
}
