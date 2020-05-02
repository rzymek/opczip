package com.github.rzymek.opczip;

import com.github.rzymek.opczip.utils.StreamGobbler;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.readAllBytes;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class LibreOfficeCompatIT {
    static File input = new File("target/test-classes/libre.xlsx");
    static File xlsxDir = new File("target/libre/");

    @BeforeAll
    static void unzip() throws IOException {
        unzip(input, xlsxDir);
    }

    @AfterAll
    static void cleanup() throws IOException {
        Files.walk(xlsxDir.toPath())
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
    }

    @Test
    void base() throws Exception {
        libreValidate(input);
    }

    protected static void unzip(File input, File target) throws IOException {
        target.mkdirs();
        ZipFile zip = new ZipFile(input);
        for (Enumeration<? extends ZipEntry> entries = zip.entries(); entries.hasMoreElements(); ) {
            ZipEntry entry = entries.nextElement();
            Path to = target.toPath().resolve(entry.getName());
            to.getParent().toFile().mkdirs();
            Files.copy(zip.getInputStream(entry), to);
        }
    }

    protected void libreValidate(File input) throws IOException, InterruptedException {
        File targetDir = new File("xlsxDir");
        File output = new File(targetDir, input.getName().replace(".xlsx", ".csv"));
        output.delete();
        assertFalse(output.exists(), output + " exists");
        Process soffice = new ProcessBuilder()
                .command("soffice", "--headless",
                        "--convert-to", "csv",
                        "--outdir", targetDir.getPath(),
                        input.getPath()
                ).start();
        ExecutorService executorService = Executors.newFixedThreadPool(2);
        executorService.submit(new StreamGobbler(soffice.getErrorStream(), System.err::println));
        executorService.submit(new StreamGobbler(soffice.getInputStream(), System.out::println));
        soffice.waitFor();
        assertEquals(
                "ok\n",
                new String(readAllBytes(output.toPath()), UTF_8)
        );
    }

    public static void zip(File sourceDirPath, File zipFilePath) throws IOException {
        Path p = Files.createFile(zipFilePath.toPath());
        try (ZipOutputStream zs = new ZipOutputStream(Files.newOutputStream(p))) {
            Path pp = sourceDirPath.toPath();
            Files.walk(pp)
                    .filter(path -> !Files.isDirectory(path))
                    .forEach(path -> {
                        ZipEntry zipEntry = new ZipEntry(pp.relativize(path).toString());
                        try {
                            zs.putNextEntry(zipEntry);
                            Files.copy(path, zs);
                            zs.closeEntry();
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    });
        }
    }

    public static void opczip(File sourceDirPath, File zipFilePath) throws IOException {
        Path p = Files.createFile(zipFilePath.toPath());
        try (OpcOutputStream zs = new OpcOutputStream(Files.newOutputStream(p))) {
            Path pp = sourceDirPath.toPath();
            Files.walk(pp)
                    .filter(path -> !Files.isDirectory(path))
                    .forEach(path -> {
                        ZipEntry zipEntry = new ZipEntry(pp.relativize(path).toString());
                        try {
                            zs.putNextEntry(zipEntry);
                            Files.copy(path, zs);
                            zs.closeEntry();
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    });
        }
    }

    @Test
    void jdk() throws Exception {
        File xlsx = new File("target/jdk.xlsx");
        xlsx.delete();
        zip(xlsxDir, xlsx);
        libreValidate(xlsx);
    }

    @Test
    void opczip() throws Exception {
        File xlsx = new File("target/opczip.xlsx");
        xlsx.delete();
        opczip(xlsxDir, xlsx);
        libreValidate(xlsx);
    }
}
