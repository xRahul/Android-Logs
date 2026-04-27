package in.rahulja.getlogs;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class LogParserBenchmarkTest {

    private static final int ITERATIONS = 5000; // Robolectric is slow, so keep iterations reasonable
    // But for a micro-optimization we need enough iterations to see the difference.
    // Since Robolectric adds overhead, the relative difference might be smaller or the noise higher.
    // However, the allocation saving is deterministic.

    @Test
    public void benchmarkLogParsing() {
        String json = "{" +
                "\"datetime\": \"2023-10-27 10:00:00\"," +
                "\"action\": \"in.rahulja.getlogs.ACTION_TEST\"," +
                "\"data\": {" +
                "  \"key1\": \"[item1, item2, item3, item4, item5, item6, item7, item8, item9, item10]\"," +
                "  \"key2\": \"[a, b, c]\"" +
                "}" +
                "}";

        // Warmup
        for (int i = 0; i < 100; i++) {
            LogParser.getLogLineForArray(json);
        }

        long startTime = System.nanoTime();
        for (int i = 0; i < ITERATIONS; i++) {
            LogParser.getLogLineForArray(json);
        }
        long endTime = System.nanoTime();

        System.out.println("Benchmark time: " + (endTime - startTime) / 1_000_000.0 + " ms for " + ITERATIONS + " iterations");
    }
}
