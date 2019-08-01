package com.github.rzymek.opczip.reader;

import org.apache.commons.compress.utils.IOUtils;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.zip.*;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ZipEntryReaderTest {
    @Test
    void testXlsx() throws IOException {
        try (ZipEntryReader reader = new ZipEntryReader(getClass().getResourceAsStream("/libre.xlsx"))) {
            ZipEntry entry = reader.nextEntry();
            assertEquals("xl/_rels/workbook.xml.rels", entry.getName());
            InputStream raw = reader.getCompressedStream();
            assertEquals(210, raw.readAllBytes().length);
            entry = reader.nextEntry();
            assertEquals("xl/sharedStrings.xml", entry.getName());
            reader.skipStream();
            entry = reader.nextEntry();
            assertEquals("xl/worksheets/sheet1.xml", entry.getName());
            assertEquals(866, reader.getCompressedStream().readAllBytes().length);
        }
    }

    @Test
    void testZip() throws IOException {
        try (ZipEntryReader reader = new ZipEntryReader(new FileInputStream("target/test.zip"))) {
            ZipEntry entry = reader.nextEntry();
            assertEquals("file_1.txt", entry.getName());
            assertArrayEquals(new byte[]{51, 52, 36, 14, 0, 0}, reader.getCompressedStream().readAllBytes());
            entry = reader.nextEntry();
            assertEquals("file_2.txt", entry.getName());
            reader.skipStream();
            entry = reader.nextEntry();
            assertEquals("file_3.txt", entry.getName());
            String s = new String(reader.getUncompressedStream().readAllBytes(), StandardCharsets.UTF_8);
            assertEquals("3333333333333333333333333333333333333333",s);
        }
    }

}


