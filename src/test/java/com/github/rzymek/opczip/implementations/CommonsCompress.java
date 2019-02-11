package com.github.rzymek.opczip.implementations;

import com.github.rzymek.opczip.base.ZipImpl;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;

import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.ZipEntry;

public class CommonsCompress extends ZipArchiveOutputStream implements ZipImpl {
    public CommonsCompress(OutputStream out) {
        super(out);
    }

    @Override
    public void putNextEntry(ZipEntry zipEntry) throws IOException {
        super.putArchiveEntry(new ZipArchiveEntry(zipEntry.getName()));
    }

    @Override
    public void closeEntry() throws IOException {
        super.closeArchiveEntry();
    }
}
