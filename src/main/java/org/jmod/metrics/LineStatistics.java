package org.jmod.metrics;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * Line / comment counts for a source file.
 */
public final class LineStatistics {
    private int loc;
    private int locom;
    private int total;

    public LineStatistics(File file) {
        try (BufferedReader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
            boolean inBlock = false;
            String line;
            while ((line = reader.readLine()) != null) {
                total++;
                String trimmed = line.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }
                if (inBlock) {
                    locom++;
                    if (trimmed.contains("*/")) {
                        inBlock = false;
                    }
                    continue;
                }
                if (trimmed.startsWith("//")) {
                    locom++;
                    continue;
                }
                if (trimmed.startsWith("/*") || trimmed.startsWith("/**")) {
                    locom++;
                    if (!trimmed.contains("*/")) {
                        inBlock = true;
                    }
                    continue;
                }
                loc++;
                if (trimmed.contains("/*") && !trimmed.contains("*/")) {
                    inBlock = true;
                    locom++;
                }
            }
        } catch (IOException ignored) {
            // leave zeros
        }
    }

    public int getLoc() {
        return loc;
    }

    public int getLocom() {
        return locom;
    }

    public int getTotal() {
        return total;
    }
}
