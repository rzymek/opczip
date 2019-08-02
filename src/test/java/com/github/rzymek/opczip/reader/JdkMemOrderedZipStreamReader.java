package com.github.rzymek.opczip.reader;

import java.io.InputStream;

class JdkMemOrderedZipStreamReader extends MemCacheOrderedZipStreamReader {
    @Override
    protected SkippableZip open(InputStream in) {
        return new JdkZipInputStream(in);
    }
}
