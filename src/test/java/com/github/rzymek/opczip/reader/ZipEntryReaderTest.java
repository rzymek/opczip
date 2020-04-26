package com.github.rzymek.opczip.reader;

import com.github.rzymek.opczip.reader.skipping.ZipStreamReader;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.zip.Deflater;
import java.util.zip.InflaterInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static com.github.rzymek.opczip.reader.OrderedZipStreamReaderTest.generateEntry;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class ZipEntryReaderTest {
    final static File testFile = new File("target", "ZipEntryReaderTest.zip");
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

    @Test
    void testXlsx() throws IOException {
        try (ZipStreamReader reader = new ZipStreamReader(getClass().getResourceAsStream("/libre.xlsx"))) {
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
    void demo() throws IOException {
        InputStream in = getClass().getResourceAsStream("/libre.xlsx");
        try (ZipStreamReader reader = new ZipStreamReader(in)) {
            byte[] sharedZipped = null;
            for (; ; ) {
                ZipEntry entry = reader.nextEntry();
                if (entry == null) {
                    break;
                }
                if ("xl/sharedStrings.xml".equals(entry.getName())) {
                    sharedZipped = reader.getCompressedStream() // save compressed entry without decompressing
                            .readAllBytes();
                } else if ("xl/worksheets/sheet1.xml".equals(entry.getName())) {
                    byte[] sheet1 = reader.getUncompressedStream() // decompress current entry
                            .readAllBytes();
                    if (sharedZipped != null) {
                        byte[] shared = ZipStreamReader
                                // decompress previously saved entry
                                .uncompressed(new ByteArrayInputStream(sharedZipped))
                                .readAllBytes();
                    }
                } else {
                    reader.skipStream(); // skip entry without decompressing
                }
            }
        }
    }

    @Test
    void testXlsxReZipped() throws IOException {
        try (ZipStreamReader reader = new ZipStreamReader(getClass().getResourceAsStream("/libre-rezipped.xlsx"))) {
            assertEquals("docProps/", reader.nextEntry().getName());
            assertEquals(0, reader.getCompressedStream().readAllBytes().length);
            assertEquals("docProps/core.xml", reader.nextEntry().getName());
            reader.skipStream();
            assertEquals("docProps/app.xml", reader.nextEntry().getName());
            final String APP_XML = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                    "<Properties xmlns=\"http://schemas.openxmlformats.org/officeDocument/2006/extended-properties\" xmlns:vt=\"http://schemas.openxmlformats.org/officeDocument/2006/docPropsVTypes\"><Template></Template><TotalTime>0</TotalTime><Application>LibreOffice/6.0.7.3$Linux_X86_64 LibreOffice_project/00m0$Build-3</Application></Properties>";
            assertEquals(APP_XML, new String(reader.getUncompressedStream().readAllBytes(), StandardCharsets.UTF_8));

            assertEquals("[Content_Types].xml", reader.nextEntry().getName());
            assertEquals(309, reader.getCompressedStream().readAllBytes().length);
            assertEquals("_rels/", reader.nextEntry().getName());
            assertEquals(0, reader.getCompressedStream().readAllBytes().length);
            assertEquals("_rels/.rels", reader.nextEntry().getName());
            assertEquals(224, reader.getCompressedStream().readAllBytes().length);
        }
    }

    @Test
    void testStreamingZip() throws IOException {
        test(ZipEntryReaderTest.testFile);
    }

    private void test(File file) throws IOException {
        try (ZipStreamReader reader = new ZipStreamReader(new FileInputStream(file))) {
            ZipEntry entry = reader.nextEntry();
            assertEquals("file_1.txt", entry.getName());
            assertEquals("1111111111111111111111111111111111111111", entryToString(reader));
            entry = reader.nextEntry();
            assertEquals("file_2.txt", entry.getName());
            reader.skipStream();
            entry = reader.nextEntry();
            assertEquals("file_3.txt", entry.getName());
            assertEquals("3333333333333333333333333333333333333333", entryToString(reader));
            assertEquals("file_4.txt", reader.nextEntry().getName());
            reader.skipStream();
            assertEquals("file_5.txt", reader.nextEntry().getName());
            assertEquals("5555555555555555555555555555555555555555", entryToString(reader));
            assertNull(reader.nextEntry());
            assertNull(reader.getCompressedStream());
            assertNull(reader.nextEntry());
        }
    }

    static String entryToString(ZipStreamReader reader) throws IOException {
        InflaterInputStream inputStream = reader.getUncompressedStream();
        return toString(inputStream);
    }

    static String toString(InputStream inputStream) throws IOException {
        return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
    }

}


