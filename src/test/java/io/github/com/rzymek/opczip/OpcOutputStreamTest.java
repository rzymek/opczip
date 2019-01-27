package io.github.com.rzymek.opczip;

import org.junit.jupiter.api.Test;

import java.io.*;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpcOutputStreamTest {
    private Map<String, String> contents = createContents();

    @Test
    void test() throws IOException {

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        create(out, contents);
        out.close();

        byte[] zipFile = out.toByteArray();
        assertThat(zipFile.length, greaterThan(0));
        validate(new ByteArrayInputStream(zipFile), contents);
    }

    private void validate(InputStream in, Map<String, String> contents) throws IOException {
        ZipInputStream zip = new ZipInputStream(in);
        Set<String> leftToRead = new HashSet<>(contents.keySet());
        for (; ; ) {
            ZipEntry entry = zip.getNextEntry();
            if (entry == null) {
                break;
            }
            assertEquals(contents.get(entry.getName()), readFully(zip));
            assertTrue(leftToRead.remove(entry.getName()));
            zip.closeEntry();
        }
        assertThat(leftToRead, empty());
    }

    private void create(ByteArrayOutputStream out, Map<String, String> contents) throws IOException {
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
        for (int i = 0; i < 10; i++) {
            String name = String.format("dir%s/file%s.txt", i % 3, i);
            contents.put(name, "this is the contents of " + name);
        }
        return contents;
    }

}