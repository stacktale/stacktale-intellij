package io.github.gabrielbbaldez.stacktale.idea;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;

/**
 * Project-level settings for the Stacktale tool window: which log file to read, and how often
 * to re-read it. The defaults are today's behaviour — an empty path auto-detects
 * {@code errors-ai.log}, and the interval is the poll the plugin has always used — so a
 * project that never opens the settings page notices nothing.
 *
 * <p>This is the persisted state; the rules it applies live in {@link StSettings}, where they
 * are unit-tested. Values are read on every poll rather than captured once; see
 * {@link StacktaleReportService}.
 */
@Service(Service.Level.PROJECT)
@State(name = "StacktaleSettings", storages = @Storage("stacktale.xml"))
public final class StacktaleSettings implements PersistentStateComponent<StacktaleSettings.State> {

    /** The serialized shape: public mutable fields are what the XML serializer works with. */
    public static final class State {
        public String logPath = StSettings.DEFAULT_LOG_PATH;
        public int pollSeconds = StSettings.DEFAULT_POLL_SECONDS;
    }

    private final Project project;
    private State state = new State();

    public StacktaleSettings(@NotNull Project project) {
        this.project = project;
    }

    static @NotNull StacktaleSettings getInstance(@NotNull Project project) {
        return project.getService(StacktaleSettings.class);
    }

    @Override
    public @NotNull State getState() {
        return state;
    }

    @Override
    public void loadState(@NotNull State loaded) {
        this.state = loaded;
    }

    /** The configured path as typed, trimmed. Empty means auto-detect. */
    @NotNull String logPath() {
        return StSettings.normalizeLogPath(state.logPath);
    }

    void setLogPath(@Nullable String logPath) {
        state.logPath = StSettings.normalizeLogPath(logPath);
    }

    int pollSeconds() {
        return StSettings.clampPollSeconds(state.pollSeconds);
    }

    void setPollSeconds(int pollSeconds) {
        state.pollSeconds = StSettings.clampPollSeconds(pollSeconds);
    }

    int pollMillis() {
        return pollSeconds() * 1000;
    }

    /**
     * The configured log file, or null when the path is empty (auto-detect) or unusable.
     * Whether the file exists is the caller's question.
     */
    @Nullable Path configuredLog() {
        return StSettings.resolveLogPath(state.logPath, project.getBasePath());
    }
}
