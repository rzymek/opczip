package com.github.rzymek.opczip.reader.skipping;

import com.github.rzymek.opczip.reader.InputStreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

class ReadFullyInflaterInputStream extends InflaterInputStream {

    public ReadFullyInflaterInputStream(InputStream compressedStream) {
        super(new PushbackInputStream(compressedStream, 512), new Inflater(true));
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int read = super.read(b, off, len);
        if (read == -1) {
            readEnd();
        }
        return read;
    }

    private void readEnd() throws IOException {
        // read till end of original stream
        int n = inf.getRemaining();
        if (n > 0) {
            ((PushbackInputStream) in).unread(buf, this.len - n, n);
        }
        InputStreamUtils.readAllBytes(in);
    }
}
