package com.github.rzymek.opczip.reader;

import com.github.rzymek.opczip.reader.skipping.ZipStreamReader;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.assertj.core.api.Assertions.assertThat;

public class FileListTest implements ArgumentsProvider {

    public static final String XLSX_DIR = "/xlsx/";

    @ParameterizedTest
    @ArgumentsSource(FileListTest.class)
    void shouldListSameFilenamesAsJDK(String filename) throws IOException {
        String opc = listWithOpc(filename);
        String jdk = listWithJDK(filename);
        assertThat(opc).isEqualTo(jdk);
    }

    private String listWithOpc(String filename) throws IOException {
        List<String> entries = new ArrayList<>();
        try (ZipStreamReader reader = new ZipStreamReader(open(filename))) {
            for (; ; ) {
                ZipEntry entry = reader.nextEntry();
                if (entry == null) {
                    break;
                }
                entries.add(entry.getName() + ":" + entry.getCompressedSize());
                reader.skipStream();
            }
        }
        return String.join("\n", entries);
    }

    private String listWithJDK(String filename) throws IOException {
        List<String> entries = new ArrayList<>();
        try (ZipInputStream zip = new ZipInputStream(open(filename))) {
            for (; ; ) {
                ZipEntry entry = zip.getNextEntry();
                if (entry == null) {
                    break;
                }
                entries.add(entry.getName() + ":" + entry.getCompressedSize());
                zip.closeEntry();
            }
        }
        return String.join("\n", entries);
    }

    private InputStream open(String filename) {
        return getClass().getResourceAsStream(XLSX_DIR + filename);
    }

    @Override
    public Stream<? extends Arguments> provideArguments(ExtensionContext extensionContext) {
        InputStream resource = FileListTest.class.getResourceAsStream(XLSX_DIR);
        Scanner scanner = new Scanner(resource);
        return StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(new Iterator<>() {
                    @Override
                    public boolean hasNext() {
                        boolean hasNext = scanner.hasNext();
                        if (!hasNext) {
                            scanner.close();
                        }
                        return hasNext;
                    }

                    @Override
                    public Arguments next() {
                        return Arguments.of(scanner.nextLine());
                    }
                }, Spliterator.ORDERED),
                false
        );
    }
}
