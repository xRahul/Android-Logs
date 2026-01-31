package in.rahulja.getlogs;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 30)
public class LogReaderBenchmarkTest {

    private File tempFile;
    private static final int FILE_SIZE_MB = 10;
    private static final String LOG_LINE = "{\"datetime\":\"2023-10-27 10:00:00\", \"action\":\"Test.Action\", \"data\":{\"key\":\"value\"}}\n";

    @Before
    public void setUp() throws IOException {
        tempFile = File.createTempFile("benchmark_logs", ".txt");
        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            long size = 0;
            byte[] bytes = LOG_LINE.getBytes();
            while (size < FILE_SIZE_MB * 1024 * 1024) {
                fos.write(bytes);
                size += bytes.length;
            }
        }
    }

    @Test
    public void benchmarkOldLoadMethod() throws IOException {
        long startTime = System.nanoTime();

        ArrayList<String> logArray = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(tempFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                // Mocking LogParser logic simply by adding the line directly to isolate IO/Memory cost
                // In real app, LogParser adds overhead, but here we test the "load all lines" strategy.
                logArray.add(line);
            }
            Collections.reverse(logArray);
        }

        long duration = System.nanoTime() - startTime;
        System.out.println("Old Method Load Time (" + FILE_SIZE_MB + "MB): " + duration / 1_000_000.0 + " ms");
        System.out.println("Items loaded: " + logArray.size());
    }

    @Test
    public void benchmarkNewLoadMethod() throws IOException {
        long startTime = System.nanoTime();

        // Simulate MainActivity's new logic: Load first page using ReverseLogReader
        try (ReverseLogReader reader = new ReverseLogReader(tempFile)) {
            // Page size 100
            java.util.List<String> rawLines = reader.readLines(100);
            ArrayList<String> logArray = new ArrayList<>();
            for (String line : rawLines) {
                 // Mocking LogParser logic
                 logArray.add(line);
            }
        }

        long duration = System.nanoTime() - startTime;
        System.out.println("New Method Load Time (" + FILE_SIZE_MB + "MB, first 100 lines): " + duration / 1_000_000.0 + " ms");
    }
}
