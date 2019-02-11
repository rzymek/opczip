package com.github.rzymek.opczip.implementations;

import com.github.rzymek.opczip.OpcOutputStream;
import com.github.rzymek.opczip.base.ZipImpl;

import java.io.OutputStream;

public class OpcZip extends OpcOutputStream implements ZipImpl {

    public OpcZip(OutputStream out) {
        super(out);
    }
}
