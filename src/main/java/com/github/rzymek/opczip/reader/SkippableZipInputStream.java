package com.github.rzymek.opczip.reader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface SkippableZipInputStream extends AutoCloseable {
    @Override
    void close() throws IOException;

    void skipEntry() throws IOException;

    String getNextEntry() throws IOException;

    void closeEntry() throws IOException;

    InputStream getInputStream();

    void transferCompressedTo(OutputStream outputStream) throws IOException;
}
