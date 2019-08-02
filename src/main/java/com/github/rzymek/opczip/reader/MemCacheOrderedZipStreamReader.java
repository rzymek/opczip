package com.github.rzymek.opczip.reader;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class MemCacheOrderedZipStreamReader extends OrderedZipStreamReader {
    private Map<String, byte[]> cache = new HashMap<>();

    protected SkippableZip open(InputStream in) {
        return new SkippableZipReader(in);
    }

    @Override
    protected OutputStream getTempOutputStream(String name) {
        return new ByteArrayOutputStream() {
            @Override
            public void close() throws IOException {
                super.close();
                cache.put(name, this.toByteArray());
            }
        };
    }

    @Override
    protected InputStream getTempInputStream(String name) throws UncheckedIOException {
        byte[] buf = cache.get(name);
        if (buf == null) {
            throw new IllegalStateException("No cache for " + name);
        }
        return new ByteArrayInputStream(buf);
    }
}

