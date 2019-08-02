package com.github.rzymek.opczip.reader.skip.zip;

import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;
import java.util.zip.ZipEntry;

import static com.github.rzymek.opczip.reader.skip.zip.ZipReadSpec.*;

public class ZipReader implements AutoCloseable {
    private final PushbackInputStream in;
    private int flag;
    private boolean reachedCEN = false;
    private ZipEntry currentEntry;

    public ZipReader(InputStream in) {
        this.in = new PushbackInputStream(in, 8192);
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
                    "Got " + Signature.toString(lfh, LFH.length());
            throw new IOException(msg);
        }
        flag = get16(lfh, LOCFLG);
        final int nameLen = get16(lfh, LOCNAM);
        byte[] filename = readNBytes(in, nameLen);
        currentEntry = new ZipEntry(new String(filename, StandardCharsets.US_ASCII));

        currentEntry.setCompressedSize(get32(lfh, LOCSIZ));
        currentEntry.setSize(get32(lfh, LOCLEN));
        currentEntry.setCrc(get32(lfh, LOCCRC));

        int extLen = get16(lfh, LOCEXT);
        in.skip(extLen);
        return currentEntry;
    }


    public void skipStream() throws IOException {
        long compressedSize = currentEntry.getCompressedSize();
        if (compressedSize > 0) {
            in.skip(compressedSize + (expectingDatSig() ? DAT_SIZE : 0));
        } else {
            getCompressedStream().readAllBytes();
        }
    }

    public InflaterInputStream getUncompressedStream() {
        return uncompressed(getCompressedStream());
    }

    public static InflaterInputStream uncompressed(InputStream compressedStream) {
        return new InflaterInputStream(compressedStream, new Inflater(true));
    }

    public InputStream getCompressedStream() {
        if (reachedCEN) {
            return null;
        }
        return new CompressedEntryInputStream(in, expectingDatSig());
    }

    private boolean expectingDatSig() {
        return (flag & DATA_DESCRIPTOR_USED) != 0;
    }

    @Override
    public void close() throws IOException {
        in.close();
    }

}

