package com.github.rzymek.opczip.base;

import java.io.IOException;
import java.util.zip.ZipEntry;

public interface ZipImpl extends AutoCloseable {

    void setLevel(int level);

    void putNextEntry(ZipEntry zipEntry) throws IOException;

    void write(byte[] buf, int i, int length) throws IOException;

    void closeEntry() throws IOException;
}
