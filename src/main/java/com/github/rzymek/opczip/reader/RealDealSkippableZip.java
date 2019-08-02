package com.github.rzymek.opczip.reader;

import com.github.rzymek.opczip.reader.skip.zip.ZipReader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class RealDealSkippableZip implements SkippableZipInputStream {
    final ZipReader in;

    public RealDealSkippableZip(InputStream in) {
        this.in = new ZipReader(in);
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
    public void closeEntry() throws IOException {

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
    public InputStream uncompressedTransferred(InputStream tempInputStream) {
        return ZipReader.uncompressed(tempInputStream);
    }
}
