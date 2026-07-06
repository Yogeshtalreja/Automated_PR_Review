package com.pr.review.reviewbot.diff;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DiffParser {

    // Matches lines like: @@ -10,4 +10,6 @@
    private static final Pattern HUNK_HEADER =
            Pattern.compile("@@\\s+-\\d+(?:,\\d+)?\\s+\\+(\\d+)(?:,\\d+)?\\s+@@");

    public static List<DiffLine> parse(String patch) {
        List<DiffLine> lines = new ArrayList<>();
        if (patch == null || patch.isBlank()) {
            return lines; // binary files have no patch
        }

        int fileLineNumber = 0;
        int diffPosition = 0;

        for (String line : patch.split("\n")) {
            Matcher matcher = HUNK_HEADER.matcher(line);

            if (matcher.find()) {
                // New hunk — reset the file line counter to where this hunk starts
                fileLineNumber = Integer.parseInt(matcher.group(1)) - 1;
                diffPosition++;
                continue;
            }

            diffPosition++;

            if (line.startsWith("+")) {
                fileLineNumber++;
                lines.add(new DiffLine(fileLineNumber, diffPosition, "ADDED", line.substring(1)));
            } else if (line.startsWith("-")) {
                // Removed lines don't exist in the new file, so no fileLineNumber increment
                lines.add(new DiffLine(fileLineNumber, diffPosition, "REMOVED", line.substring(1)));
            } else {
                fileLineNumber++;
                String content = line.startsWith(" ") ? line.substring(1) : line;
                lines.add(new DiffLine(fileLineNumber, diffPosition, "CONTEXT", content));
            }
        }

        return lines;
    }
}