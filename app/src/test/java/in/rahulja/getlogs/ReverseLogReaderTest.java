package in.rahulja.getlogs;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ReverseLogReaderTest {

    private File tempFile;

    @Before
    public void setUp() throws IOException {
        tempFile = File.createTempFile("test_logs", ".txt");
    }

    @After
    public void tearDown() {
        if (tempFile != null && tempFile.exists()) {
            tempFile.delete();
        }
    }

    private void writeToFile(String content) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            fos.write(content.getBytes(StandardCharsets.UTF_8));
        }
    }

    @Test
    public void testReadLines() throws IOException {
        writeToFile("Line1\nLine2\nLine3");

        try (ReverseLogReader reader = new ReverseLogReader(tempFile)) {
            List<String> lines = reader.readLines(10);
            assertEquals(3, lines.size());
            assertEquals("Line3", lines.get(0));
            assertEquals("Line2", lines.get(1));
            assertEquals("Line1", lines.get(2));
        }
    }

    @Test
    public void testReadLinesWithLimit() throws IOException {
        writeToFile("Line1\nLine2\nLine3\nLine4");

        try (ReverseLogReader reader = new ReverseLogReader(tempFile)) {
            List<String> batch1 = reader.readLines(2);
            assertEquals(2, batch1.size());
            assertEquals("Line4", batch1.get(0));
            assertEquals("Line3", batch1.get(1));

            List<String> batch2 = reader.readLines(2);
            assertEquals(2, batch2.size());
            assertEquals("Line2", batch2.get(0));
            assertEquals("Line1", batch2.get(1));
        }
    }

    @Test
    public void testReadLinesAcrossBufferBoundary() throws IOException {
        // Create a line larger than buffer (8192) or just use many lines
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            sb.append("Line").append(i).append("\n");
        }
        writeToFile(sb.toString());

        try (ReverseLogReader reader = new ReverseLogReader(tempFile)) {
            List<String> lines = reader.readLines(1000);
            assertEquals(1000, lines.size());
            assertEquals("Line999", lines.get(0));
            assertEquals("Line0", lines.get(999));
        }
    }

    @Test
    public void testEmptyFile() throws IOException {
        writeToFile("");
        try (ReverseLogReader reader = new ReverseLogReader(tempFile)) {
            List<String> lines = reader.readLines(10);
            assertTrue(lines.isEmpty());
        }
    }

    @Test
    public void testSingleLineNoNewline() throws IOException {
        writeToFile("Line1");
        try (ReverseLogReader reader = new ReverseLogReader(tempFile)) {
            List<String> lines = reader.readLines(10);
            assertEquals(1, lines.size());
            assertEquals("Line1", lines.get(0));
        }
    }

    @Test
    public void testMultipleNewlinesAtEnd() throws IOException {
        writeToFile("Line1\n\n");
        try (ReverseLogReader reader = new ReverseLogReader(tempFile)) {
            List<String> lines = reader.readLines(10);
            // "Line1\n\n" -> Last newline ignored -> "Line1", ""
            assertEquals(2, lines.size());
            assertEquals("", lines.get(0));
            assertEquals("Line1", lines.get(1));
        }
    }
}
