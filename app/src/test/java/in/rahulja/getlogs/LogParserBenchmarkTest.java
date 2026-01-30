package in.rahulja.getlogs;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import static org.junit.Assert.assertNotNull;

@RunWith(RobolectricTestRunner.class)
public class LogParserBenchmarkTest {

    private static final int ITERATIONS = 100000;

    // Sample JSON that triggers both split paths:
    // 1. "items": "[...]" triggers the array split.
    // 2. "wifiInfo": "..." triggers the wifiInfo split.
    private static final String JSON_INPUT = "{" +
            "\"datetime\": \"2023-10-27 10:00:00\"," +
            "\"action\": \"in.rahulja.getlogs.ACTION_TEST\"," +
            "\"data\": {" +
            "  \"items\": \"[item1, item2, item3, item4, item5, item6, item7, item8, item9, item10]\"," +
            "  \"wifiInfo\": \"ssid, bssid, signal, freq, capability, extra1, extra2, extra3\"" +
            "}" +
            "}";

    @Test
    public void benchmarkLogParsing() {
        // Warm up
        for (int i = 0; i < 1000; i++) {
            LogParser.getLogLineForArray(JSON_INPUT);
        }

        long startTime = System.nanoTime();
        for (int i = 0; i < ITERATIONS; i++) {
            String result = LogParser.getLogLineForArray(JSON_INPUT);
            assertNotNull(result); // prevent dead code elimination
        }
        long duration = System.nanoTime() - startTime;

        String msg = "Benchmark Duration: " + duration / 1_000_000.0 + " ms";
        System.out.println(msg);
        System.out.println("Iterations: " + ITERATIONS);
        System.out.println("Average time per call: " + (duration / (double)ITERATIONS) + " ns");
    }
}
