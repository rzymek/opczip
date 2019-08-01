package com.github.rzymek.opczip.reader;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;
import java.util.zip.ZipEntry;

public class ZipEntryReader implements AutoCloseable {
    private static final int DATA_DESCRIPTOR_USED = 0x08;

    static final int LOCHDR = 30;       // LOC header size
    static final int DATLEN = 16;       // LOC header size
    static long LOCSIG32 = 0x04034b50L;   // "PK\003\004"
    static final int LOCVER = 4;        // version needed to extract
    static final int LOCFLG = 6;        // general purpose bit flag
    static final int LOCHOW = 8;        // compression method
    static final int LOCTIM = 10;       // modification time
    static final int LOCCRC = 14;       // uncompressed file crc-32 value
    static final int LOCSIZ = 18;       // compressed size
    static final int LOCLEN = 22;       // uncompressed size
    static final int LOCNAM = 26;       // filename length
    static final int LOCEXT = 28;       // extra field length


    private final PushbackInputStream in;
    private int flag;
    private int locSigIdx = 0;
    private int datSigIdx = 0;

    public ZipEntryReader(InputStream in) {
        this.in = new PushbackInputStream(in, 8192);
    }

    public ZipEntry nextEntry() throws IOException {
        byte[] loc = readNBytes(LOCHDR);
        if (get32(loc, 0) != LOCSIG32) {
            throw new IOException("expecting LOC signature");
        }
        flag = get16(loc, LOCFLG);
        final int nameLen = (int) get32(loc, LOCNAM);
        byte[] filename = readNBytes(nameLen);
        ZipEntry e = new ZipEntry(new String(filename, StandardCharsets.US_ASCII));

        e.setCompressedSize(get32(loc, LOCSIZ));
        e.setSize(get32(loc, LOCLEN));
        e.setCrc(get32(loc, LOCCRC));

        int extLen = get16(loc, LOCEXT);
        in.skip(extLen);
        return e;
    }

    private byte[] readNBytes(int len) throws IOException {
        byte[] buf = new byte[len];
        int read = in.readNBytes(buf, 0, len);
        if (read != len) {
            throw new IOException("EOF while reading LOC");
        }
        return buf;
    }

    static byte[] LOCSIG = {80, 75, 3, 4};   // "PK\003\004"
    static byte[] DATSIG = {80, 75, 7, 8};   // "PK\007\008"

    public InflaterInputStream getUncompressedStream() {
        return uncompressed(getCompressedStream());
    }

    protected static InflaterInputStream uncompressed(InputStream compressedStream) {
        return new InflaterInputStream(compressedStream, new Inflater(true));
    }

    public InputStream getCompressedStream() {
        return new FilterInputStream(in) {
            boolean eof = false;

            @Override
            public int read() throws IOException {
                byte[] b = new byte[1];
                int read = read(b, 0, 1);
                return read == 1 ? b[0] : read;
            }

            @Override
            public int read(byte[] b, int off, int len) throws IOException {
                if (eof) {
                    return -1;
                }
                int read = super.read(b, off, len);
                for (int i = 0; i < len; i++) {
                    if (b[off + i] == DATSIG[datSigIdx]) {
                        datSigIdx++;
                        if (datSigIdx >= DATSIG.length) {
                            eof(b, off, len, i - DATSIG.length + 1);
                            in.skip(DATLEN);
                            datSigIdx = 0;
                            return i - DATSIG.length + 1;
                        }
                    } else {
                        datSigIdx = 0;
                    }
                    if (b[off + i] == LOCSIG[locSigIdx]) {
                        locSigIdx++;
                        if (locSigIdx >= LOCSIG.length) {
                            eof(b, off, len, i);
                            locSigIdx = 0;
                            return i - LOCSIG.length + 1;
                        }
                    } else {
                        locSigIdx = 0;
                    }
                }
                return read;
            }

            protected void eof(byte[] b, int off, int len, int i) throws IOException {
                ZipEntryReader.this.in.unread(b, off + i, len - i);
                eof = true;
            }
        };
    }

    public void skipStream() throws IOException {
        getCompressedStream().readAllBytes();
    }

    @Override
    public void close() throws IOException {
        in.close();
    }

    /**
     * Fetches unsigned 16-bit value from byte array at specified offset.
     * The bytes are assumed to be in Intel (little-endian) byte order.
     */
    public static final int get16(byte b[], int off) {
        return (b[off] & 0xff) | ((b[off + 1] & 0xff) << 8);
    }

    /**
     * Fetches unsigned 32-bit value from byte array at specified offset.
     * The bytes are assumed to be in Intel (little-endian) byte order.
     */
    public static final long get32(byte b[], int off) {
        return (get16(b, off) | ((long) get16(b, off + 2) << 16)) & 0xffffffffL;
    }

    /**
     * Fetches signed 64-bit value from byte array at specified offset.
     * The bytes are assumed to be in Intel (little-endian) byte order.
     */
    public static final long get64(byte b[], int off) {
        return get32(b, off) | (get32(b, off + 4) << 32);
    }

    /**
     * Fetches signed 32-bit value from byte array at specified offset.
     * The bytes are assumed to be in Intel (little-endian) byte order.
     *
     */

}
