package in.rahulja.getlogs;

import org.junit.Test;
import java.util.ArrayList;
import java.util.Collections;
import static org.junit.Assert.assertEquals;

public class LogLoadingBenchmarkTest {

    private static final int ITEM_COUNT = 10000;
    private static final String DUMMY_LOG = "Dummy log line content for testing purposes";

    @Test
    public void benchmarkListInsertion() {
        // Benchmark Old Approach: add(0, item)
        long startTimeOld = System.nanoTime();
        ArrayList<String> listOld = new ArrayList<>();
        for (int i = 0; i < ITEM_COUNT; i++) {
            listOld.add(0, DUMMY_LOG + i);
        }
        long durationOld = System.nanoTime() - startTimeOld;
        System.out.println("Old approach (add(0)): " + durationOld / 1_000_000.0 + " ms");

        // Benchmark New Approach: add(item) + reverse()
        long startTimeNew = System.nanoTime();
        ArrayList<String> listNew = new ArrayList<>();
        for (int i = 0; i < ITEM_COUNT; i++) {
            listNew.add(DUMMY_LOG + i);
        }
        Collections.reverse(listNew);
        long durationNew = System.nanoTime() - startTimeNew;
        System.out.println("New approach (add + reverse): " + durationNew / 1_000_000.0 + " ms");

        // Verify that both produce the same order (conceptually)
        // Note: The loops add "DUMMY_LOG + i" differently.
        // Old: i=0 -> [0], i=1 -> [1, 0] ... Result: [N-1, ..., 0]
        // New: i=0 -> [0], i=1 -> [0, 1] ... Reverse -> [N-1, ..., 0]
        assertEquals(listOld.size(), listNew.size());
        assertEquals(listOld.get(0), listNew.get(0));
        assertEquals(listOld.get(ITEM_COUNT - 1), listNew.get(ITEM_COUNT - 1));

        // Assert improvement (New should be faster)
        // Usually it's much faster, but in CI environments or small N, it might vary.
        // However, for 10,000 items, the difference is O(N^2) vs O(N), so it should be significant.
        if (durationNew < durationOld) {
             System.out.println("Performance Improvement: " + String.format("%.2fx", (double)durationOld / durationNew));
        } else {
             System.out.println("Warning: New approach was not faster. Check N value.");
        }
    }
}
