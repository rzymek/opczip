package com.github.rzymek.opczip;

import com.github.rzymek.opczip.base.BenchmarkBase;
import com.github.rzymek.opczip.base.ZipImpl;
import com.github.rzymek.opczip.implementations.CommonsCompress;
import com.github.rzymek.opczip.implementations.JdkZip;
import com.github.rzymek.opczip.implementations.Noop;
import com.github.rzymek.opczip.implementations.OpcZip;
import org.openjdk.jmh.annotations.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Random;
import java.util.function.Function;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.assertEquals;

@BenchmarkMode(Mode.SingleShotTime)
@Measurement(iterations = 1)
@Warmup(iterations = 0)
@Fork(1)
public class BenchmarksIT extends BenchmarkBase {

    public static final int SIZE = 1_000_000;

    @Benchmark
    public long noop() throws Exception {
        return createBigZip("noop", Noop::new, 0);
    }

    @Benchmark
    public long jdk0() throws Exception {
        return createBigZip("jdk", JdkZip::new, 0);
    }

    @Benchmark
    public long jdk() throws Exception {
        return createBigZip("jdk", JdkZip::new, Deflater.DEFAULT_COMPRESSION);
    }

    @Benchmark
    public long jdkBestSpeed() throws Exception {
        return createBigZip("jdk", JdkZip::new, Deflater.BEST_SPEED);
    }

    @Benchmark
    public long opczip() throws Exception {
        return createBigZip("opczip", OpcZip::new, Deflater.DEFAULT_COMPRESSION);
    }

    @Benchmark
    public long opczipBestSpeed() throws Exception {
        return createBigZip("opczip", OpcZip::new, Deflater.BEST_SPEED);
    }


    @Benchmark
    public long commons() throws Exception {
        return createBigZip("commons", CommonsCompress::new, Deflater.DEFAULT_COMPRESSION);
    }

    @Benchmark
    public long commonsBestSpeed() throws Exception {
        return createBigZip("commons", CommonsCompress::new, Deflater.BEST_SPEED);
    }

    public long createBigZip(String name, Function<OutputStream, ZipImpl> createZip, int level) throws Exception {
        Random random = new Random(0);
        File file = new File("target", name + "_" + level + ".zip");
        try (
                OutputStream out = new FileOutputStream(file);
                ZipImpl zip = createZip.apply(out)
        ) {
            zip.setLevel(level);
            zip.putNextEntry(new ZipEntry("big.dat"));
            byte[] buf = new byte[8 * 1024];
            for (int i = 0; i < SIZE / 8; i++) {
                random.nextBytes(buf);
                zip.write(buf, 0, buf.length);
            }
            zip.closeEntry();
        }
        assertEquals(SIZE * 1024L, new ZipFile(file).getEntry("big.dat").getSize());
        return file.length();
    }
}

