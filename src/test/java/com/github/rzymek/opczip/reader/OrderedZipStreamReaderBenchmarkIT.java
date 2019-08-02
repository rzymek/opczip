package com.github.rzymek.opczip.reader;

import org.junit.jupiter.api.*;

import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static com.github.rzymek.opczip.reader.OrderedZipStreamReaderTest.generateEntry;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class OrderedZipStreamReaderBenchmarkIT {
    final static File testFile = new File("target", "bin-read.zip");
    private static final int SIZE = 1024 * 1024 * 10;
    public static final int REPEATS = 15;
    private boolean file4;
    private boolean file2;

    @BeforeAll
    static void createTestFile() throws Exception {
        if (testFile.exists()) {
            return;
        }
        try (ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(testFile))) {
            generateEntry(zip, '1', SIZE * 100);
            generateEntry(zip, '2', SIZE);
            generateEntry(zip, '3', SIZE * 100);
            generateEntry(zip, '4', SIZE);
            generateEntry(zip, '5', SIZE * 200);
            zip.closeEntry();
        }
    }

    @AfterAll
    static void cleanup() {
//        testFile.delete();
    }

    @RepeatedTest(REPEATS) @Disabled
    void baseline() throws IOException {
        try (ZipInputStream in = new ZipInputStream(new FileInputStream(testFile))) {
            for (; ; ) {
                ZipEntry nextEntry = in.getNextEntry();
                if(nextEntry == null){
                    break;
                }
                if(nextEntry.getName().equals("file_5.txt")){
                    break;
                }
                System.out.println(nextEntry.getName());
                in.closeEntry();
            }
        }
    }

    @RepeatedTest(REPEATS)
    void shouldReadInOrderMem() throws Exception {
        file2 = false;
        file4 = false;
        new MemCacheOrderedZipStreamReader()
                .with(this::file2, "file_2.txt", "file_4.txt")
                .with(this::file4, "file_4.txt")
                .read(new FileInputStream(testFile));
        assertTrue(file4);
        assertTrue(file2);
    }

    @RepeatedTest(REPEATS)
    void shouldReadInOrderDisk() throws Exception {
        file2 = false;
        file4 = false;
        new DiskCacheOrderedZipStreamReader()
                .with(this::file2, "file_2.txt", "file_4.txt")
                .with(this::file4, "file_4.txt")
                .read(new FileInputStream(testFile));
        assertTrue(file4);
        assertTrue(file2);
    }

    @RepeatedTest(REPEATS) @Disabled
    void shouldReadInOrderJdk() throws Exception {
        file2 = false;
        file4 = false;
        new JdkMemOrderedZipStreamReader()
                .with(this::file2, "file_2.txt", "file_4.txt")
                .with(this::file4, "file_4.txt")
                .read(new FileInputStream(testFile));
        assertTrue(file4);
        assertTrue(file2);
    }

    void file4(InputStream in) throws IOException {
        assertFalse(file4);
        assertFalse(file2);
        file4 = true;
    }

    void file2(InputStream in) throws IOException {
        assertFalse(file2);
        assertTrue(file4);
        file2 = true;
    }
}

