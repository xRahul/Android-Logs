package in.rahulja.getlogs;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Robolectric;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

@RunWith(RobolectricTestRunner.class)
public class PerformanceTest {

    @Test
    public void testReadLogsPerformance() throws IOException {
        // Setup
        File tempFile = File.createTempFile("perf_test_logs", ".txt");
        tempFile.deleteOnExit();

        int lineCount = 100000;
        try (FileWriter writer = new FileWriter(tempFile)) {
            for (int i = 0; i < lineCount; i++) {
                writer.write("{\"datetime\": \"2023-10-27 10:00:00\", \"action\": \"TEST_ACTION_" + i + "\", \"data\": {\"key\": \"value\"}}\n");
            }
        }

        MainActivity activity = Robolectric.buildActivity(MainActivity.class).get();

        // Measure
        long startTime = System.nanoTime();
        ArrayList<String> result = activity.readLogsFromFile(tempFile);
        long endTime = System.nanoTime();

        long durationMs = (endTime - startTime) / 1000000;
        System.out.println("Performance Benchmark: Reading " + lineCount + " lines took " + durationMs + " ms");

        // Assert
        if (result.size() != lineCount) {
             throw new RuntimeException("Expected " + lineCount + " lines, but got " + result.size());
        }
    }
}
