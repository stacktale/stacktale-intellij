package io.github.gabrielbbaldez.stacktale.idea;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
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
        this.alarm = new Alarm(Alarm.ThreadToUse.POOLED_THREAD, this);
        alarm.addRequest(this::poll, 0);
    }

    static @NotNull StacktaleReportService getInstance(@NotNull Project project) {
        return project.getService(StacktaleReportService.class);
    }

    void addListener(
            @NotNull Listener listener,
            @NotNull Disposable parent
    ) {
        listeners.add(listener);
        Disposer.register(parent, () -> listeners.remove(listener));
        listener.reportsChanged(currentLog, currentReports);
    }

    void refreshNow() {
        if (disposed || project.isDisposed()) return;
        alarm.addRequest(() -> refresh(true), 0);
    }

    private void poll() {
        if (disposed || project.isDisposed()) return;

        refresh(false);

        if (!disposed && !project.isDisposed()) {
            alarm.addRequest(this::poll, POLL_MILLIS);
        }
    }

    private synchronized void refresh(boolean force) {
        if (disposed || project.isDisposed()) return;

        Path log = findLog();

        // A nested log cannot be resolved while project indexes are unavailable.
        // Preserve the current state and let the next poll retry after indexing.
        if (log == null && DumbService.getInstance(project).isDumb()) return;

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

        if (!force && log.equals(currentLog) && content.equals(lastContent)) return;

        currentLog = log;
        lastContent = content;
        currentReports = List.copyOf(StReportParser.parse(content));
        notifyListeners();
    }

    private void notifyListeners() {
        Path log = currentLog;
        List<StReport> reports = currentReports;

        ApplicationManager.getApplication().invokeLater(() -> {
            if (disposed || project.isDisposed()) return;

            for (Listener listener : listeners) {
                listener.reportsChanged(log, reports);
            }
        });
    }

    /** Prefer ./errors-ai.log; otherwise use an indexed file in the project. */
    private @Nullable Path findLog() {
        String base = project.getBasePath();
        if (base != null) {
            Path candidate = Path.of(base, "errors-ai.log");
            if (Files.isRegularFile(candidate)) return candidate;
        }

        if (DumbService.getInstance(project).isDumb()) return null;

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
