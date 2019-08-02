package com.github.rzymek.opczip.reader;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class DiskCacheOrderedZipStreamReader extends OrderedZipStreamReader {
    private final File dir;

    public DiskCacheOrderedZipStreamReader() throws IOException {
        dir = Files.createTempDirectory(DiskCacheOrderedZipStreamReader.class.getSimpleName()).toFile();
    }

    @Override
    protected OutputStream getTempOutputStream(String name) throws FileNotFoundException {
        File file = new File(dir, name);
        file.deleteOnExit();
        return new FileOutputStream(file);
    }

    @Override
    protected InputStream getTempInputStream(String name) throws UncheckedIOException {
        try {
            File file = new File(dir, name);
            return new FileInputStream(file) {
                @Override
                public void close() throws IOException {
                    super.close();
                    file.delete();
                }
            };
        } catch (FileNotFoundException e) {
            throw new UncheckedIOException(e);
        }
    }
}

