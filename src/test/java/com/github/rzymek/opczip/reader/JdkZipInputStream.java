package com.github.rzymek.opczip.reader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class JdkZipInputStream implements SkippableZip {
    private final ZipInputStream in;

    public JdkZipInputStream(InputStream in) {
        this.in = new ZipInputStream(in);
    }

    @Override
    public void close() throws IOException {
        in.close();
    }

    @Override
    public void skipEntry() throws IOException {
        in.readAllBytes();
    }

    @Override
    public String getNextEntry() throws IOException {
        ZipEntry nextEntry = in.getNextEntry();
        return nextEntry == null ? null : nextEntry.getName();
    }


    @Override
    public InputStream getUncompressedInputStream() {
        return in;
    }

    @Override
    public void transferCompressedTo(OutputStream outputStream) throws IOException {
        getUncompressedInputStream().transferTo(outputStream);
        outputStream.close();
    }

    @Override
    public InputStream uncompress(InputStream tempInputStream) {
        return tempInputStream;
    }
}
