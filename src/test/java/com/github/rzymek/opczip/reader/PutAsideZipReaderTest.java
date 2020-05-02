package com.github.rzymek.opczip.reader;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class PutAsideZipReaderTest {

    final static File testFile = new File("target", PutAsideZipReaderTest.class.getName() + ".zip");
    private static final int SIZE = 40;

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

    static void generateEntry(ZipOutputStream zip, char c, int size) throws IOException {
        zip.putNextEntry(new ZipEntry("file_" + c + ".txt"));
        byte[] buf = new byte[size];
        Arrays.fill(buf, (byte) c);
        zip.write(buf, 0, buf.length);
    }

    @Test
    void test() throws Exception {
        try (PutAsideZipStreamReader reader = new PutAsideZipStreamReader(new FileInputStream(testFile))) {
            assertEquals("file_1.txt", reader.nextEntry().getName());
            assertEquals("1111111111111111111111111111111111111111", ZipEntryReaderTest.toString(reader.getInputStream()));
            ZipEntry zipEntry = reader.nextEntry();
            assertEquals("file_2.txt", zipEntry.getName());
            reader.putAsideForLater();
            assertEquals("file_3.txt", reader.nextEntry().getName());
            reader.skipEntry();
            assertEquals("file_4.txt", reader.nextEntry().getName());
            reader.putAsideForLater();
            assertEquals("file_5.txt", reader.nextEntry().getName());
            assertEquals("5555555555555555555555555555555555555555", ZipEntryReaderTest.toString(reader.getInputStream()));
            assertEquals("file_2.txt", reader.nextEntry().getName());
            reader.skipEntry();
            assertEquals("file_4.txt", reader.nextEntry().getName());
            assertEquals("4444444444444444444444444444444444444444", ZipEntryReaderTest.toString(reader.getInputStream()));
            assertNull(reader.nextEntry());
            assertNull(reader.nextEntry());
        }
    }
}
