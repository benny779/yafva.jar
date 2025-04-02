package il.co.outburn.rest;

import lombok.extern.slf4j.Slf4j;

import java.io.OutputStream;
import java.io.PrintStream;

@Slf4j
public class FhirLogPrintStream extends PrintStream {

    private final boolean stderr;

    public FhirLogPrintStream(OutputStream out, boolean stderr) {
        super(out);
        this.stderr = stderr;
    }

    private boolean skip(String line) {
        return "|".equals(line) ||
                ".".equals(line) ||
                "10".equals(line) ||
                "20".equals(line) ||
                "30".equals(line) ||
                "40".equals(line) ||
                "50".equals(line) ||
                "60".equals(line) ||
                "70".equals(line) ||
                "80".equals(line) ||
                "90".equals(line) ||
                "100".equals(line) ||
                "110".equals(line) ||
                "120".equals(line) ||
                "140".equals(line) ||
                "160".equals(line) ||
                "180".equals(line);
    }

    @Override
    public void println(String line) {
        if (skip(line)) return;
        if (stderr) {
            log.error(line);
        } else {
            log.info(line);
        }
    }

    @Override
    public void print(String s) {
        if (skip(s)) return;
        if (stderr) {
            log.error(s);
        } else {
            log.info(s);
        }
    }

    @Override
    public void print(Object obj) {
        if (stderr) {
            log.error(obj.toString());
        } else {
            log.info(obj.toString());
        }
    }

    @Override
    public void println() {
    }

    @Override
    public void println(Object x) {
        if (stderr) {
            log.error(x.toString());
        } else {
            log.info(x.toString());
        }
    }
}

