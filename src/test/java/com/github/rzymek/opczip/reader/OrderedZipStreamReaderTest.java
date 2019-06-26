package com.github.rzymek.opczip.reader;

import com.github.rzymek.opczip.reader.OrderedZipStreamReader;
import org.apache.commons.compress.utils.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class OrderedZipStreamReaderTest {
    final static File testFile = new File("target", "test.zip");
    private String file4;
    private String file2;

    @BeforeEach
    void createTestFile() throws Exception {
        try (ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(testFile))) {
            generateEntry(zip, '1');
            generateEntry(zip, '2');
            generateEntry(zip, '3');
            generateEntry(zip, '4');
            generateEntry(zip, '5');
            zip.closeEntry();
        }
        file2 = null;
        file4 = null;
    }

    @Test
    void shouldRead() throws Exception {
        OrderedZipStreamReader
                .with(this::file4, "file_4.txt")
                .read(new FileInputStream(testFile));
        assertEquals("4444444444444444444444444444444444444444", file4);
    }


    @Test
    void shouldReadInOrder() throws Exception {
        OrderedZipStreamReader
                .with(this::file2, "file_2.txt", "file_4.txt")
                .with(this::file4, "file_4.txt")
                .read(new FileInputStream(testFile));
        assertEquals("4444444444444444444444444444444444444444", file4);
        assertEquals(
                "4444444444444444444444444444444444444444\n" +
                        "2222222222222222222222222222222222222222",
                file2
        );
    }

    void file4(InputStream in) throws IOException {
        file4 = toString(in);
    }

    void file2(InputStream in) throws IOException {
        file2 = file4 + "\n" + toString(in);
    }

    static void generateEntry(ZipOutputStream zip, char c) throws IOException {
        zip.putNextEntry(new ZipEntry("file_" + c + ".txt"));
        byte[] buf = new byte[40];
        Arrays.fill(buf, (byte) c);
        zip.write(buf, 0, buf.length);
    }

    static String toString(InputStream in) throws IOException {
        return new String(IOUtils.toByteArray(in), StandardCharsets.US_ASCII);
    }
}

