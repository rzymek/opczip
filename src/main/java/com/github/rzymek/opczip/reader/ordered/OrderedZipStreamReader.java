package com.github.rzymek.opczip.reader.ordered;

import com.github.rzymek.opczip.reader.skipping.ZipStreamReader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;

import static java.util.stream.Collectors.toSet;

public abstract class OrderedZipStreamReader {
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
        try (ZipStreamReader zip = open(in)) {
            while (hasPendingConsumers()) {
                ZipEntry entry = zip.nextEntry();
                if (entry == null) {
                    break;
                }
                String name = entry.getName();
                ConsumerEntry consumer = consumers.get(name);
                if (consumer == null) {
                    zip.skipStream();
                } else {
                    if (isEveryConsumed(consumer.dependencies)) {
                        process(zip.getUncompressedStream(), consumer);
                        consumers.entrySet().stream()
                                .filter(e -> !e.getValue().consumed)
                                .filter(e -> e.getValue().dependencies.contains(name))
                                .filter(e -> isEveryConsumedBut(e.getValue().dependencies, name))
                                .forEach(e -> process(ZipStreamReader.uncompressed(getTempInputStream(e.getKey())), e.getValue()));
                    } else {
                        try (OutputStream out = getTempOutputStream(name)) {
                            zip.getCompressedStream().transferTo(out);
                        }
                    }
                }
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

    protected ZipStreamReader open(InputStream in) {
        return new ZipStreamReader(in);
    }

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
