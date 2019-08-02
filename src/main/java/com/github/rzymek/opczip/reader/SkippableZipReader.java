package com.github.rzymek.opczip.reader;

import com.github.rzymek.opczip.reader.skipping.ZipStreamReader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

class SkippableZipReader implements SkippableZip {
    final ZipStreamReader in;

    public SkippableZipReader(InputStream in) {
        this.in = new ZipStreamReader(in);
    }

    @Override
    public void close() throws IOException {
        in.close();
    }

    @Override
    public void skipEntry() throws IOException {
        in.skipStream();
    }

    @Override
    public String getNextEntry() throws IOException {
        return in.nextEntry().getName();
    }


    @Override
    public InputStream getUncompressedInputStream() {
        return in.getUncompressedStream();
    }

    @Override
    public void transferCompressedTo(OutputStream outputStream) throws IOException {
        in.getCompressedStream().transferTo(outputStream);
        outputStream.close();
    }

    @Override
    public InputStream uncompress(InputStream tempInputStream) {
        return ZipStreamReader.uncompressed(tempInputStream);
    }
}
