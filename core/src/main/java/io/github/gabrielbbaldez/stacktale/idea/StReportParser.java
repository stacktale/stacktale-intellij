package io.github.gabrielbbaldez.stacktale.idea;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses the st/1 text in {@code errors-ai.log} into {@link StReport}s. Pure — no IntelliJ
 * dependencies — so it is unit-tested headless. Tolerant of unknown lines (forward
 * compatibility, per the st/1 spec), of the self-describing file header (which quotes the
 * delimiter mid-line), and of truncated / rotated files.
 */
public final class StReportParser {

    private static final String START = "━━━ ERROR #";
    private static final String END = "━━━ END #";
    // id + timestamp from "━━━ ERROR #a1b2 ━━━ 2026-07-10 20:16:40.412 thread=… ━━━"
    private static final Pattern HEADER = Pattern.compile("^━━━ ERROR #(\\S+) ━━━ (.+?) thread=");
    // a frame carrying a source location: "…(PaymentService.java:44)"
    private static final Pattern FRAME = Pattern.compile(
            "\\(([^\\s:()]+\\.(?:java|kts|kt|groovy|scala)):(-?\\d+)\\)");

    private StReportParser() {
    }

    public static List<StReport> parse(String content) {
        List<StReport> reports = new ArrayList<>();
        if (content == null || content.isEmpty()) return reports;
        String[] lines = content.split("\n", -1);
        int i = 0;
        while (i < lines.length) {
            // a real report starts with the delimiter at column 0; the header only quotes it
            // mid-line inside a '#' comment, so startsWith is enough to tell them apart
            if (!stripCr(lines[i]).startsWith(START)) {
                i++;
                continue;
            }
            List<String> block = new ArrayList<>();
            block.add(stripCr(lines[i]));
            boolean complete = false;
            int j = i + 1;
            while (j < lines.length) {
                String bl = stripCr(lines[j]);
                if (bl.startsWith(START)) break; // next block began — this one was truncated
                block.add(bl);
                if (bl.startsWith(END)) {
                    complete = true;
                    j++;
                    break;
                }
                j++;
            }
            if (complete) {
                StReport report = parseBlock(block);
                if (report != null) reports.add(report);
            }
            i = j;
        }
        return reports;
    }
    private static StReport parseBlock(List<String> block) {
        Matcher h = HEADER.matcher(block.get(0));
        if (!h.find()) return null;
        String id = h.group(1);
        String timestamp = h.group(2).trim();
        String headline = block.size() > 1 ? block.get(1).trim() : "";

        StFrame culprit = null;
        boolean markedFrameFound = false;
        List<StFrame> frames = new ArrayList<>();
        for (String bl : block) {
            Matcher fm = FRAME.matcher(bl);
            if (fm.find()) {
                boolean marked = bl.contains("← YOUR CODE") || bl.contains("← culprit");
                if (marked) markedFrameFound = true;

                int line = parseLineNumber(fm.group(2));
                if (line <= 0) continue;

                StFrame frame = new StFrame(fm.group(1), line, bl.trim());
                frames.add(frame);
                if (culprit == null && marked) culprit = frame;
            }
        }
        if (culprit == null && !markedFrameFound && !frames.isEmpty()) {
            culprit = frames.get(0);
        }
        return new StReport(id, timestamp, headline, culprit, frames, String.join("\n", block));
    }
    private static int parseLineNumber(String value) {
        try {
            long line = Long.parseLong(value);
            if (line > Integer.MAX_VALUE) return Integer.MAX_VALUE;
            if (line < Integer.MIN_VALUE) return Integer.MIN_VALUE;
            return (int) line;
        } catch (NumberFormatException ignored) {
            // The regex guarantees digits, so overflow is the only expected failure.
            return value.startsWith("-") ? Integer.MIN_VALUE : Integer.MAX_VALUE;
        }
    }
    private static String stripCr(String s) {
        return s.endsWith("\r") ? s.substring(0, s.length() - 1) : s;
    }
}
