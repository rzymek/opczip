package com.github.rzymek.opczip.reader;

import com.github.rzymek.opczip.reader.skipping.ZipStreamReader;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.AbstractMap;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.zip.InflaterInputStream;
import java.util.zip.ZipEntry;

import static com.github.rzymek.opczip.reader.InputStreamUtils.readAllBytes;

public class PutAsideZipStreamReader implements AutoCloseable {
    private final ZipStreamReader in;
    private Deque<Map.Entry<ZipEntry, byte[]>> saved = new ArrayDeque<>();
    private byte[] currentRestored = null;
    private boolean finished = false;
    private ZipEntry entry;

    public PutAsideZipStreamReader(InputStream in) {
        this.in = new ZipStreamReader(in);
    }

    public InflaterInputStream getInputStream() {
        if (currentRestored == null) {
            return in.getUncompressedStream();
        } else {
            return ZipStreamReader.uncompressed(new ByteArrayInputStream(currentRestored));
        }
    }


    public ZipEntry nextEntry() throws IOException {
        if (finished) {
            if (saved.isEmpty()) {
                this.currentRestored = null;
                return null;
            } else {
                Map.Entry<ZipEntry, byte[]> restored = saved.removeFirst();
                this.currentRestored = restored.getValue();
                return restored.getKey();
            }
        } else {
            entry = in.nextEntry();
            if (entry == null) {
                finished = true;
                return nextEntry();
            } else {
                return entry;
            }
        }
    }

    public void putAsideForLater() throws IOException {
        saved.addLast(new AbstractMap.SimpleEntry<>(entry, readAllBytes(in.getCompressedStream())));
    }

    public void skipEntry() throws IOException {
        if (!finished) {
            in.skipStream();
        }
    }

    @Override
    public void close() throws IOException {
        in.close();
    }
}
