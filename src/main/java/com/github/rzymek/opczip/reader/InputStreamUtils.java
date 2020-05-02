package com.github.rzymek.opczip.reader;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class InputStreamUtils {
    public static void discardAllBytes(InputStream in) throws IOException {
        int bufSize = 2048;
        byte[] buffer = new byte[bufSize];
        while (in.read(buffer, 0, bufSize) >= 0) {
            // ignore
        }
    }

    public static byte[] readAllBytes(InputStream in) throws IOException {
        // in Java9
        // return in.readAllBytes();
        try (final ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            transferTo(in, out);
            return out.toByteArray();
        }
    }

    public static void transferTo(InputStream in, OutputStream out) throws IOException {
        // in Java9:
        // in.transferTo(out);
        int bufSize = 4096;
        byte[] buffer = new byte[bufSize];
        int read;
        while ((read = in.read(buffer, 0, bufSize)) >= 0) {
            out.write(buffer, 0, read);
        }
    }
}
