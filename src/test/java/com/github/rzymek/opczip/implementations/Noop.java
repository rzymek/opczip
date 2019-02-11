package com.github.rzymek.opczip.implementations;

import com.github.rzymek.opczip.base.ZipImpl;

import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.ZipEntry;

public class Noop implements ZipImpl {

    public Noop(OutputStream outputStream) {

    }

    @Override
    public void setLevel(int level) {

    }

    @Override
    public void putNextEntry(ZipEntry zipEntry) throws IOException {

    }

    @Override
    public void write(byte[] buf, int i, int length) throws IOException {

    }

    @Override
    public void closeEntry() throws IOException {

    }

    @Override
    public void close() throws Exception {

    }
}
