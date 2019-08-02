package com.github.rzymek.opczip.reader.skip.zip;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.PushbackInputStream;

import static com.github.rzymek.opczip.reader.skip.zip.ZipReadSpec.*;

class CompressedEntryInputStream extends FilterInputStream {
    private final SignatureMatcher cen = new SignatureMatcher(CEN);
    private final SignatureMatcher lfh = new SignatureMatcher(LFH);
    private final SignatureMatcher dat = new SignatureMatcher(DAT);

    private final boolean expectingDatSig;
    private boolean endOfEntry = false;

    public CompressedEntryInputStream(PushbackInputStream in, boolean expectingDatSig) {
        super(in);
        this.expectingDatSig = expectingDatSig;
    }

    @Override
    public int read() throws IOException {
        byte[] b = new byte[1];
        int read = read(b, 0, 1);
        return read == 1 ? b[0] : read;
    }

    @Override
    public int read(byte[] buf, int off, int len) throws IOException {
        if (endOfEntry) {
            return -1;
        }
        int readCount = super.read(buf, off, len);
        for (int i = 0; i < len; i++) {
            byte currentByte = buf[off + i];
            if (expectingDatSig && dat.matchNext(currentByte)) {
                eof(buf, off, len, i - DAT.length() + 1);
                in.skip(DAT_SIZE);
                return i - DAT.length() + 1;
            }
            if (lfh.matchNext(currentByte) || cen.matchNext(currentByte)) {
                eof(buf, off, len, i - LFH.length() + 1);
                return i - LFH.length() + 1;
            }
        }
        return readCount;
    }

    private void eof(byte[] buf, int off, int len, int i) throws IOException {
        ((PushbackInputStream) in).unread(buf, off + i, len - i);
        endOfEntry = true;
    }

}
