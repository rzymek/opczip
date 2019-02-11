package com.github.rzymek.opczip.implementations;

import com.github.rzymek.opczip.base.ZipImpl;

import java.io.OutputStream;
import java.util.zip.ZipOutputStream;

public class JdkZip extends ZipOutputStream implements ZipImpl {
    public JdkZip(OutputStream out) {
        super(out);
    }
}
