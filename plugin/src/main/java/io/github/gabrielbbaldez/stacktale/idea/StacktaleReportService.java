package io.github.gabrielbbaldez.stacktale.idea;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.Alarm;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Project-level source of Stacktale reports.
 *
 * Owns the single errors-ai.log poll used by both the tool window and status-bar widget.
 */
@Service(Service.Level.PROJECT)
public final class StacktaleReportService implements Disposable {

    private static final int POLL_MILLIS = 3000;

    interface Listener {
        void reportsChanged(@Nullable Path log, @NotNull List<StReport> reports);
    }

    private final Project project;
    private final Alarm alarm;
    private final List<Listener> listeners = new CopyOnWriteArrayList<>();

    private volatile Path currentLog;
    private volatile List<StReport> currentReports = List.of();
    private String lastContent;
    private volatile boolean disposed;

    public StacktaleReportService(@NotNull Project project) {
        this.project = project;
        this.alarm = new Alarm(Alarm.ThreadToUse.SWING_THREAD, this);
        alarm.addRequest(this::poll, 0);
    }

    static @NotNull StacktaleReportService getInstance(@NotNull Project project) {
        return project.getService(StacktaleReportService.class);
    }

    void addListener(@NotNull Listener listener) {
        listeners.add(listener);
        listener.reportsChanged(currentLog, currentReports);
    }

    void removeListener(@NotNull Listener listener) {
        listeners.remove(listener);
    }

    void refreshNow() {
        lastContent = null;
        refresh();
    }

    private void poll() {
        if (disposed || project.isDisposed()) return;

        refresh();

        if (!disposed && !project.isDisposed()) {
            alarm.addRequest(this::poll, POLL_MILLIS);
        }
    }

    private void refresh() {
        Path log = findLog();

        if (log == null) {
            boolean changed = currentLog != null
                    || lastContent != null
                    || !currentReports.isEmpty();

            currentLog = null;
            currentReports = List.of();
            lastContent = null;

            if (changed) notifyListeners();
            return;
        }

        String content;
        try {
            content = Files.readString(log, StandardCharsets.UTF_8);
        } catch (Exception ignored) {
            return;
        }

        if (log.equals(currentLog) && content.equals(lastContent)) return;

        currentLog = log;
        lastContent = content;
        currentReports = List.copyOf(StReportParser.parse(content));
        notifyListeners();
    }

    private void notifyListeners() {
        Path log = currentLog;
        List<StReport> reports = currentReports;

        for (Listener listener : listeners) {
            listener.reportsChanged(log, reports);
        }
    }

    /** Prefer ./errors-ai.log; otherwise use an indexed file in the project. */
    private @Nullable Path findLog() {
        String base = project.getBasePath();
        if (base != null) {
            Path candidate = Path.of(base, "errors-ai.log");
            if (Files.isRegularFile(candidate)) return candidate;
        }

        Collection<VirtualFile> found = ReadAction.compute(() ->
                FilenameIndex.getVirtualFilesByName(
                        "errors-ai.log",
                        GlobalSearchScope.projectScope(project)
                ));

        for (VirtualFile file : found) {
            Path path = Path.of(file.getPath());
            if (Files.isRegularFile(path)) return path;
        }

        return null;
    }

    @Override
    public void dispose() {
        disposed = true;
        listeners.clear();
        alarm.cancelAllRequests();
    }
}
