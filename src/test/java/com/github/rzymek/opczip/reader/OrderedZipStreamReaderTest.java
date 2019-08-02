package com.github.rzymek.opczip.reader;

import com.github.rzymek.opczip.reader.ordered.MemCacheOrderedZipStreamReader;
import org.apache.commons.compress.utils.IOUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class OrderedZipStreamReaderTest {
    final static File testFile = new File("target", "test.zip");
    public static final int SIZE = 1024 * 1024;
    public static final String FILE4 = create('4');
    public static final String FILE2 = create('2');

    private static String create(char c) {
        char[] buf = new char[SIZE];
        Arrays.fill(buf, c);
        return new String(buf);
    }

    private String file4;
    private String file2;

    @BeforeAll
    static void createTestFile() throws Exception {
        try (ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(testFile))) {
            zip.setLevel(Deflater.BEST_COMPRESSION);
            generateEntry(zip, '1', SIZE);
            generateEntry(zip, '2', SIZE);
            generateEntry(zip, '3', SIZE);
            generateEntry(zip, '4', SIZE);
            generateEntry(zip, '5', SIZE);
            zip.closeEntry();
        }
    }

    @Test
    void shouldRead() throws Exception {
        file4 = null;
        new MemCacheOrderedZipStreamReader()
                .with(this::file4, "file_4.txt")
                .read(new FileInputStream(testFile));
        assertEquals(FILE4, file4);
    }


    @RepeatedTest(5)
    void shouldReadInOrder() throws Exception {
        file2 = null;
        file4 = null;
        new MemCacheOrderedZipStreamReader()
                .with(this::file2, "file_2.txt", "file_4.txt")
                .with(this::file4, "file_4.txt")
                .read(new FileInputStream(testFile));
        assertEquals(FILE4, file4);
        assertEquals(FILE4 + "\n" + FILE2, file2);
    }


    @Test
    void shouldRequireConsumersForAllDependencies() throws IOException {
        try {
            new MemCacheOrderedZipStreamReader()
                    .with(this::file2, "file_2.txt", "file_5.txt")
                    .with(this::file4, "file_4.txt")
                    .read(new ByteArrayInputStream(new byte[100]));
            fail("Exception expected");
        } catch (IllegalStateException ex) {
            assertEquals("file_2.txt has a dependencies that are not registered for processing: [file_5.txt]", ex.getMessage());
        }
    }

    void file4(InputStream in) throws IOException {
        file4 = toString(in);
    }

    void file2(InputStream in) throws IOException {
        file2 = file4 + "\n" + toString(in);
    }

    static void generateEntry(ZipOutputStream zip, char c, int size) throws IOException {
        zip.putNextEntry(new ZipEntry("file_" + c + ".txt"));
        byte[] buf = new byte[size];
        Arrays.fill(buf, (byte) c);
        zip.write(buf, 0, buf.length);
    }

    static String toString(InputStream in) throws IOException {
        return new String(IOUtils.toByteArray(in), StandardCharsets.US_ASCII);
    }
}

