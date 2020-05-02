package com.github.rzymek.opczip.reader.skipping;

import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.InflaterInputStream;
import java.util.zip.ZipEntry;

import static com.github.rzymek.opczip.reader.InputStreamUtils.discardAllBytes;
import static com.github.rzymek.opczip.reader.skipping.ExactIO.skipExactly;
import static com.github.rzymek.opczip.reader.skipping.ZipReadSpec.*;

public class ZipStreamReader implements AutoCloseable {
    private final PushbackInputStream in;
    private int flag;
    private boolean reachedCEN = false;
    protected ZipEntry currentEntry;

    public ZipStreamReader(InputStream in) {
        this.in = new PushbackInputStream(in, 8192);
    }

    public static InflaterInputStream uncompressed(InputStream compressedStream) {
        return new ReadFullyInflaterInputStream(compressedStream);
    }

    public ZipEntry nextEntry() throws IOException {
        if (reachedCEN) {
            return null;
        }
        byte[] lfh = readNBytes(in, LFH_SIZE);
        if (CEN.matchesStartOf(lfh)) {
            reachedCEN = true;
            return null;
        }
        if (!LFH.matchesStartOf(lfh)) {
            String msg = "Expecting LFH bytes (" + LFH + "). " +
                    "Got " + Signature.toString(lfh, LFH_SIZE);
            throw new IOException(msg);
        }
        flag = get16(lfh, LFH_FLG);
        final int nameLen = get16(lfh, LFH_NAM);
        byte[] filename = readNBytes(in, nameLen);

        currentEntry = new ZipEntry(new String(filename, StandardCharsets.US_ASCII));
        long csize = get32(lfh, LFH_SIZ);
        currentEntry.setCompressedSize(csize);
        long size = get32(lfh, LFH_LEN);
        currentEntry.setSize(size);
        currentEntry.setCrc(get32(lfh, LFH_CRC));

        int extLen = get16(lfh, LFH_EXT);
        skipExactly(in, extLen);

        return currentEntry;
    }

    public void skipStream() throws IOException {
        long compressedSize = currentEntry.getCompressedSize();
        if (compressedSize > 0) {
            skipExactly(in, compressedSize + (expectingDatSig() ? DAT_SIZE : 0));
        } else {
            discardAllBytes(getCompressedStream());
        }
    }

    public InflaterInputStream getUncompressedStream() {
        return uncompressed(getCompressedStream());
    }

    public InputStream getCompressedStream() {
        if (reachedCEN) {
            return null;
        }
        return new CompressedEntryInputStream(in, currentEntry, expectingDatSig());
    }

    private boolean expectingDatSig() {
        return (flag & DATA_DESCRIPTOR_USED) != 0;
    }

    @Override
    public void close() throws IOException {
        in.close();
    }

}

