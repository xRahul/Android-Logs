package in.rahulja.getlogs;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

public class WriteLogWorkerBenchmarkTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void benchmarkFileOperations() throws IOException {
        int ITERATIONS = 1000;
        String fileName = "test.log";
        String data = "log data";

        // Baseline
        long startBaseline = System.nanoTime();
        for (int i = 0; i < ITERATIONS; i++) {
            File logFolder = new File(folder.getRoot(), "AllLogsBaseline");
            if (!logFolder.exists()) {
                logFolder.mkdirs();
            }

            File myFile = new File(logFolder, fileName);
            if (!myFile.exists()) {
                myFile.createNewFile();
            }

            try (OutputStreamWriter myOutWriter = new OutputStreamWriter(
                    new FileOutputStream(myFile, true))) {
                myOutWriter.append(data);
                myOutWriter.append("\n");
            }
        }
        long durationBaseline = System.nanoTime() - startBaseline;
        System.out.println("Baseline: " + durationBaseline / 1_000_000.0 + " ms");


        // Optimized
        long startOptimized = System.nanoTime();
        for (int i = 0; i < ITERATIONS; i++) {
            File logFolder = new File(folder.getRoot(), "AllLogsOptimized");
            // Optimistic write: assume folder exists
            File myFile = new File(logFolder, fileName);

            try {
                try (FileOutputStream fos = new FileOutputStream(myFile, true);
                     OutputStreamWriter myOutWriter = new OutputStreamWriter(fos)) {
                    myOutWriter.append(data);
                    myOutWriter.append("\n");
                }
            } catch (FileNotFoundException e) {
                // Folder might be missing
                if (!logFolder.exists()) {
                    if (logFolder.mkdirs()) {
                        // Retry
                         try (FileOutputStream fos = new FileOutputStream(myFile, true);
                             OutputStreamWriter myOutWriter = new OutputStreamWriter(fos)) {
                            myOutWriter.append(data);
                            myOutWriter.append("\n");
                        }
                    } else {
                        throw e;
                    }
                } else {
                    throw e;
                }
            }
        }
        long durationOptimized = System.nanoTime() - startOptimized;
        System.out.println("Optimized: " + durationOptimized / 1_000_000.0 + " ms");

        if (durationOptimized < durationBaseline) {
             System.out.println("Speedup: " + String.format("%.2fx", (double)durationBaseline / durationOptimized));
        } else {
             System.out.println("No speedup observed (Optimized: " + durationOptimized + " vs Baseline: " + durationBaseline + ")");
        }
    }
}
