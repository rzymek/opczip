package com.github.rzymek.opczip.reader;

import com.github.rzymek.opczip.reader.skipping.ZipStreamReader;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class PutAsideZipStreamReader extends ZipStreamReader {
    private Map<String, byte[]> saved = new HashMap<>();

    public PutAsideZipStreamReader(InputStream in) {
        super(in);
    }

    public void saveStream() throws IOException {
        saved.put(currentEntry.getName(), getCompressedStream().readAllBytes());
    }

    public InputStream restoreStream(String name) {
        return uncompressed(new ByteArrayInputStream(saved.get(name)));
    }
}
