package com.github.rzymek.opczip.base;

import org.junit.jupiter.api.Test;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.io.File;
import java.util.regex.Pattern;

public abstract class BenchmarkBase {

    @Test
    public void run() throws RunnerException {
        File dir = new File("target/csv/");
        dir.mkdirs();
        Options options = new OptionsBuilder()
                .include(Pattern.quote(getClass().getName()))
                .resultFormat(ResultFormatType.CSV)
                .result(dir + "/" + getClass().getSimpleName() + ".csv")
                .build();
        new Runner(options).run();
    }
}
