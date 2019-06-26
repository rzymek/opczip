package com.github.rzymek.opczip.reader;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

class OrderedZipStreamReader {
    public static class OrderedZipStreamReaderBuilder {
        private Map<String, ConsumerEntry> consumers = new HashMap<>();

        private static class ConsumerEntry {
            Consumer consumer;
            List<String> dependencies;
            boolean consumed = false;

            ConsumerEntry(Consumer consumer, String... dependencies) {
                this.consumer = consumer;
                this.dependencies = Arrays.asList(dependencies);
            }
        }

        public OrderedZipStreamReaderBuilder with(Consumer consumer, String entry, String... dependencies) {
            ConsumerEntry prev = consumers.put(entry, new ConsumerEntry(consumer, dependencies));
            if (prev != null) {
                throw new IllegalStateException(entry + " already registered");
            }
            return this;
        }

        public void read(InputStream in) throws IOException {
            try (ZipInputStream zip = new ZipInputStream(in)) {
                while (hasPendingConsumers()) {
                    ZipEntry nextEntry = zip.getNextEntry();
                    if (nextEntry == null) {
                        break;
                    }
                    String name = nextEntry.getName();
                    ConsumerEntry entry = consumers.get(name);
                    if (entry != null) {
                        entry.consumer.accept(zip);
                        entry.consumed = true;
                    }
                    zip.closeEntry();
                }
            }
        }

        private boolean hasPendingConsumers() {
            return consumers.values().stream().anyMatch(e -> !e.consumed);
        }

    }

    public static OrderedZipStreamReaderBuilder with(Consumer consumer, String entry, String... dependencies) {
        return new OrderedZipStreamReaderBuilder().with(consumer, entry, dependencies);
    }

    @FunctionalInterface
    public interface Consumer {
        void accept(InputStream in) throws IOException;
    }
}
