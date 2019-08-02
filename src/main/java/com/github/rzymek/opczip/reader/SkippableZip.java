package com.github.rzymek.opczip.reader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

interface SkippableZip extends AutoCloseable {
    @Override
    void close() throws IOException;

    void skipEntry() throws IOException;

    String getNextEntry() throws IOException;

    InputStream getUncompressedInputStream();

    void transferCompressedTo(OutputStream outputStream) throws IOException;

    InputStream uncompress(InputStream tempInputStream);
}
