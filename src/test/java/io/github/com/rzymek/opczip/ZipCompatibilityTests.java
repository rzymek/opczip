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
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ZipCompatibilityTests {
    private Map<String, String> contents = createContents();

    @Test
    void test() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        create(out, contents);
        out.close();

        byte[] zipBytes = out.toByteArray();
        assertThat(zipBytes.length, greaterThan(0));
        validate(new ByteArrayInputStream(zipBytes));

        Path file = Files.createTempFile(Paths.get("target"), "test", ".zip");
        try {
            Files.write(file, zipBytes);
            validateZipFile(file.toFile());
        }finally {
            file.toFile().delete();
        }
    }

    private void validate(InputStream in) throws IOException {
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

    private void create(OutputStream out, Map<String, String> contents) throws IOException {
        try (OpcOutputStream zip = new OpcOutputStream(out)) {
            for (Map.Entry<String, String> entry : contents.entrySet()) {
                zip.putNextEntry(new ZipEntry(entry.getKey()));
                PrintStream printer = new PrintStream(zip);
                printer.print(entry.getValue());
                printer.flush();
                zip.closeEntry();
            }
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