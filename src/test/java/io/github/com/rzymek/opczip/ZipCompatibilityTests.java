package io.github.com.rzymek.opczip;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ZipCompatibilityTests {
    private final Map<String, String> contents = createContents();

    @Test
    void direct() throws IOException {
        validate(generate(this::createDirect));
    }

    @Test
    void wrapper() throws IOException {
        validate(generate(this::createWithWrapper));
    }

    private void validate(byte[] zipBytes) throws IOException {
        assertThat(zipBytes.length, greaterThan(0));
        validateCommonsCompress(new ByteArrayInputStream(zipBytes));
        validateUsingZipFile(zipBytes);
    }

    private byte[] generate(Consumer<OutputStream> consumer) throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            consumer.accept(out);
            return out.toByteArray();
        }
    }

    private void validateUsingZipFile(byte[] zipBytes) throws IOException {
        Path file = Files.createTempFile(Paths.get("target"), "test", ".zip");
        try {
            Files.write(file, zipBytes);
            validateZipFile(file.toFile());
        } finally {
            file.toFile().delete();
        }
    }

    private void validateCommonsCompress(InputStream in) throws IOException {
        ZipArchiveInputStream zip = new ZipArchiveInputStream(in);
        Set<String> leftToRead = new HashSet<>(contents.keySet());
        for (; ; ) {
            ArchiveEntry entry = zip.getNextEntry();
            if (entry == null) {
                break;
            }
            assertEquals(contents.get(entry.getName()), readFully(zip));
            assertTrue(leftToRead.remove(entry.getName()));
        }
        assertThat(leftToRead, empty());
    }

    private void validateZipFile(File file) throws IOException {
        ZipFile zipFile = new ZipFile(file);
        Set<String> leftToRead = new HashSet<>(contents.keySet());
        zipFile.stream().forEach(entry -> {
            try {
                InputStream in = zipFile.getInputStream(entry);
                assertEquals(contents.get(entry.getName()), readFully(in));
                assertTrue(leftToRead.remove(entry.getName()));
            } catch (IOException ex) {
                throw new UncheckedIOException(ex);
            }
        });
        assertThat(leftToRead, empty());
    }

    private void createDirect(OutputStream out) {
        try (OpcOutputStream zip = new OpcOutputStream(out)) {
            for (Map.Entry<String, String> entry : contents.entrySet()) {
                zip.putNextEntry(new ZipEntry(entry.getKey()));
                PrintStream printer = new PrintStream(zip);
                printer.print(entry.getValue());
                printer.flush();
                zip.closeEntry();
            }
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    private void createWithWrapper(OutputStream out) {
        try (ZipOutputStream zip = new OpcZipOutputStream(out)) {
            for (Map.Entry<String, String> entry : contents.entrySet()) {
                zip.putNextEntry(new ZipEntry(entry.getKey()));
                PrintStream printer = new PrintStream(zip);
                printer.print(entry.getValue());
                printer.flush();
                zip.closeEntry();
            }
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    static String readFully(InputStream in) {
        return new BufferedReader(new InputStreamReader(in))
                .lines().collect(Collectors.joining("\n"));
    }

    static Map<String, String> createContents() {
        Map<String, String> contents = new LinkedHashMap<>();
        for (int i = 0; i < 3; i++) {
            String name = String.format("dir%s/file%s.txt", i % 3, i);
            contents.put(name, "this is the contents");
        }
        return contents;
    }

}