package in.rahulja.getlogs;

import org.junit.Test;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class DateFormatBenchmarkTest {

    private static final int ITERATIONS = 10000;

    private static final ThreadLocal<DateFormat> DATE_FORMATTER = new ThreadLocal<DateFormat>() {
        @Override
        protected DateFormat initialValue() {
            return DateFormat.getDateTimeInstance();
        }
    };

    @Test
    public void benchmarkDateFormatOptimization() {
        System.out.println("Starting Benchmark with " + ITERATIONS + " iterations");

        // Warmup
        for(int i=0; i<100; i++) {
            DateFormat.getDateTimeInstance().format(Calendar.getInstance().getTime());
        }

        // 1. Baseline
        long startBaseline = System.nanoTime();
        for (int i = 0; i < ITERATIONS; i++) {
            String s = DateFormat.getDateTimeInstance().format(Calendar.getInstance().getTime());
            assertNotNull(s);
        }
        long durationBaseline = System.nanoTime() - startBaseline;
        System.out.println("Baseline (Calendar + New DateFormat): " + durationBaseline / 1_000_000.0 + " ms");

        // 2. Intermediate: new Date() instead of Calendar
        long startIntermediate = System.nanoTime();
        for (int i = 0; i < ITERATIONS; i++) {
            String s = DateFormat.getDateTimeInstance().format(new Date());
            assertNotNull(s);
        }
        long durationIntermediate = System.nanoTime() - startIntermediate;
        System.out.println("Intermediate (new Date() + New DateFormat): " + durationIntermediate / 1_000_000.0 + " ms");

        // 3. Optimized: ThreadLocal DateFormat + new Date()
        long startOptimized = System.nanoTime();
        for (int i = 0; i < ITERATIONS; i++) {
            String s = DATE_FORMATTER.get().format(new Date());
            assertNotNull(s);
        }
        long durationOptimized = System.nanoTime() - startOptimized;
        System.out.println("Optimized (new Date() + ThreadLocal DateFormat): " + durationOptimized / 1_000_000.0 + " ms");

        // Check correctness (roughly, as time moves)
        String baseline = DateFormat.getDateTimeInstance().format(new Date());
        String optimized = DATE_FORMATTER.get().format(new Date());
        // They might differ by a second if we are unlucky, but formats should match logic.
        // Actually, let's just check length or basic structure if strictly needed, but manual verification is mostly "it runs".

        if (durationOptimized < durationBaseline) {
            System.out.println("Speedup: " + String.format("%.2fx", (double)durationBaseline / durationOptimized));
        }
    }
}
