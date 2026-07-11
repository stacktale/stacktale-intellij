package io.github.gabrielbbaldez.stacktale.idea;

import java.util.List;

/**
 * One st/1 error report parsed from {@code errors-ai.log}, with just enough structure for
 * the tool window: what to show, where to jump, and the raw block to copy to an AI.
 */
public record StReport(
        String id,
        String timestamp,
        String headline,      // root-cause line, e.g. "IllegalStateException: gateway refused"
        StFrame culprit,      // the "← YOUR CODE" / "← culprit" frame, or null
        List<StFrame> frames, // every navigable frame in the block
        String block          // the full raw text block (display + copy)
) {
}
