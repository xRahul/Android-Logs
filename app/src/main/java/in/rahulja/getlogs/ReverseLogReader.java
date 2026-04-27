package in.rahulja.getlogs;

import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class ReverseLogReader implements Closeable {
    private final RandomAccessFile raf;
    private final long fileSize;
    private long currentPosition;
    private static final int BUFFER_SIZE = 8192; // 8KB
    // Stores bytes that form the end of the next line to be read (which is textually before the current chunk)
    private byte[] leftoverBytes;
    private final LinkedList<String> lineCache = new LinkedList<>();

    public ReverseLogReader(File file) throws IOException {
        this.raf = new RandomAccessFile(file, "r");
        this.fileSize = raf.length();
        this.currentPosition = fileSize;
        this.leftoverBytes = new byte[0];
    }

    public List<String> readLines(int limit) throws IOException {
        List<String> result = new ArrayList<>();

        while (result.size() < limit) {
             if (!lineCache.isEmpty()) {
                 result.add(lineCache.removeFirst());
             } else {
                 if (currentPosition <= 0 && leftoverBytes.length == 0) {
                     break; // End of file and no leftovers
                 }
                 readChunkAndFillCache();

                 // If cache is still empty after trying to fill, we must continue loop
                 // unless we are at EOF, which is handled by the if condition above.
                 if (lineCache.isEmpty() && currentPosition <= 0 && leftoverBytes.length == 0) {
                     break;
                 }
             }
        }
        return result;
    }

    private void readChunkAndFillCache() throws IOException {
        if (currentPosition == 0) {
            // We reached the start of the file, but we have leftover bytes.
            if (leftoverBytes.length > 0) {
                lineCache.add(new String(leftoverBytes, StandardCharsets.UTF_8));
                leftoverBytes = new byte[0];
            }
            return;
        }

        long bytesToReadLong = Math.min(BUFFER_SIZE, currentPosition);
        int bytesToRead = (int) bytesToReadLong;
        byte[] buffer = new byte[bytesToRead];

        raf.seek(currentPosition - bytesToRead);
        raf.readFully(buffer);

        long endOfChunkPosition = currentPosition; // Position in file where this chunk ends
        currentPosition -= bytesToRead;

        int lastNewlineIndex = bytesToRead; // Start searching from the end of the buffer

        for (int i = bytesToRead - 1; i >= 0; i--) {
            if (buffer[i] == '\n') {
                // Check if this is the very last byte of the file
                if (endOfChunkPosition == fileSize && i == bytesToRead - 1) {
                    // It's the trailing newline of the file.
                    // Ignore it for line production, but update limit so we don't include it in the next line.
                    lastNewlineIndex = i;
                    continue;
                }

                // Found a newline.
                // The bytes from i+1 to lastNewlineIndex are the start of the line we are building.
                // Combine with leftoverBytes to get the full line.
                int length = lastNewlineIndex - (i + 1);
                byte[] lineBytes = new byte[length + leftoverBytes.length];
                System.arraycopy(buffer, i + 1, lineBytes, 0, length);
                System.arraycopy(leftoverBytes, 0, lineBytes, length, leftoverBytes.length);

                String line = new String(lineBytes, StandardCharsets.UTF_8);
                lineCache.add(line);

                leftoverBytes = new byte[0]; // Clear leftover after using it
                lastNewlineIndex = i; // Update limit for next search
            }
        }

        // Finished scanning the buffer.
        // All bytes from 0 to lastNewlineIndex (exclusive) are part of the next line.
        // These bytes become the new leftover.
        int remainingLength = lastNewlineIndex;
        byte[] newLeftover = new byte[remainingLength + leftoverBytes.length];
        System.arraycopy(buffer, 0, newLeftover, 0, remainingLength);
        System.arraycopy(leftoverBytes, 0, newLeftover, remainingLength, leftoverBytes.length);
        leftoverBytes = newLeftover;
    }

    @Override
    public void close() throws IOException {
        raf.close();
    }
}
