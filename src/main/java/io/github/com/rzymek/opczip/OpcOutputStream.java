package io.github.com.rzymek.opczip;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.zip.ZipEntry;

public class OpcOutputStream extends OutputStream {

    public OpcOutputStream(OutputStream out) {
    }

    public OpcOutputStream(OutputStream out, Charset charset) {
    }

    public void setLevel(int level) {
    }

    public void putNextEntry(ZipEntry e) throws IOException {
    }

    public void closeEntry() throws IOException {
    }

    @Override
    public void write(int b) throws IOException {

    }
}
