package io.github.gabrielbbaldez.stacktale.idea;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;

/**
 * The settings rules shared by the plugin's persisted state and its settings page: the
 * defaults that reproduce the behaviour from before the settings existed, the bounds a
 * stored value is held to, and how a configured log path becomes a filesystem path.
 *
 * No IntelliJ API on purpose, so the rules are unit-testable in :core.
 */
public final class StSettings {

    /** Empty means auto-detect — what the plugin did before the path could be set. */
    public static final String DEFAULT_LOG_PATH = "";

    /** The poll the tool window has always used. */
    public static final int DEFAULT_POLL_SECONDS = 3;

    public static final int MIN_POLL_SECONDS = 1;
    public static final int MAX_POLL_SECONDS = 3600;

    private StSettings() {
    }

    /** Blank collapses to {@link #DEFAULT_LOG_PATH}; surrounding whitespace is a typo, not a path. */
    public static String normalizeLogPath(String raw) {
        return raw == null ? DEFAULT_LOG_PATH : raw.trim();
    }

    /**
     * Holds a stored interval inside the range the spinner offers. The settings file is
     * hand-editable, and a 0 that reached the poll would turn it into a busy loop.
     */
    public static int clampPollSeconds(int raw) {
        return Math.min(MAX_POLL_SECONDS, Math.max(MIN_POLL_SECONDS, raw));
    }

    /**
     * Resolves a configured log path, a relative one against the project root. Returns null
     * when nothing is configured (auto-detect) or when the text is not a usable path —
     * whether the file exists is the caller's question, not this one.
     */
    public static Path resolveLogPath(String configured, String projectBasePath) {
        String path = normalizeLogPath(configured);
        if (path.isEmpty()) return null;

        try {
            Path candidate = Path.of(path);
            if (candidate.isAbsolute()) return candidate.normalize();
            if (projectBasePath == null) return null;
            return Path.of(projectBasePath).resolve(candidate).normalize();
        } catch (InvalidPathException e) {
            return null;
        }
    }
}
