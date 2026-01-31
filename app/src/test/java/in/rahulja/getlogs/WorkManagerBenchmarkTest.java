package in.rahulja.getlogs;

import android.content.Context;
import android.content.Intent;
import androidx.test.core.app.ApplicationProvider;
import androidx.work.Configuration;
import androidx.work.testing.WorkManagerTestInitHelper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class WorkManagerBenchmarkTest {

    private AllReceivers allReceivers;
    private Context context;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();

        // Initialize WorkManager for testing
        Configuration config = new Configuration.Builder()
                .setMinimumLoggingLevel(android.util.Log.DEBUG)
                .setExecutor(new androidx.work.testing.SynchronousExecutor())
                .build();
        WorkManagerTestInitHelper.initializeTestWorkManager(context, config);

        allReceivers = new AllReceivers();
    }

    @Test
    public void benchmarkOnReceive() {
        Intent intent = new Intent("android.intent.action.BATTERY_CHANGED");
        intent.putExtra("level", 50);

        // Warm up
        allReceivers.onReceive(context, intent);

        int iterations = 100;
        long startTime = System.nanoTime();

        for (int i = 0; i < iterations; i++) {
            allReceivers.onReceive(context, intent);
        }

        long endTime = System.nanoTime();
        long durationMs = (endTime - startTime) / 1_000_000;

        System.out.println("Benchmark Duration for " + iterations + " iterations: " + durationMs + " ms");
        System.out.println("Average per iteration: " + ((double)durationMs / iterations) + " ms");
    }
}
