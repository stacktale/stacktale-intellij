package io.github.gabrielbbaldez.stacktale.idea;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.StatusBarWidget;
import com.intellij.openapi.wm.StatusBarWidgetFactory;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.util.Consumer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Component;
import java.awt.event.MouseEvent;
import java.nio.file.Path;
import java.util.List;

/** Creates the status-bar widget showing the current Stacktale report count. */
public final class StacktaleStatusBarWidgetFactory implements StatusBarWidgetFactory {

    private static final String WIDGET_ID = "StacktaleErrorCount";

    @Override
    public @NotNull String getId() {
        return WIDGET_ID;
    }

    @Override
    public @NotNull String getDisplayName() {
        return "Stacktale error count";
    }

    @Override
    public @NotNull StatusBarWidget createWidget(@NotNull Project project) {
        return new StacktaleStatusBarWidget(project);
    }

    private static final class StacktaleStatusBarWidget
            implements StatusBarWidget,
            StatusBarWidget.TextPresentation,
            StacktaleReportService.Listener {

        private final Project project;

        private volatile StatusBar statusBar;
        private volatile Path currentLog;
        private volatile int reportCount;

        private StacktaleStatusBarWidget(@NotNull Project project) {
            this.project = project;
            StacktaleReportService.getInstance(project).addListener(this, this);
        }

        @Override
        public @NotNull String ID() {
            return WIDGET_ID;
        }

        @Override
        public StatusBarWidget.WidgetPresentation getPresentation() {
            return this;
        }

        @Override
        public void install(@NotNull StatusBar statusBar) {
            this.statusBar = statusBar;
            statusBar.updateWidget(WIDGET_ID);
        }

        @Override
        public @NotNull String getText() {
            return "stacktale: " + reportCount;
        }

        @Override
        public float getAlignment() {
            return Component.CENTER_ALIGNMENT;
        }

        @Override
        public @NotNull String getTooltipText() {
            if (currentLog == null) {
                return "No errors-ai.log found in this project";
            }

            String reportWord = reportCount == 1 ? "report" : "reports";
            return reportCount + " Stacktale error " + reportWord
                    + " in " + currentLog;
        }

        @Override
        public @NotNull Consumer<MouseEvent> getClickConsumer() {
            return event -> {
                if (project.isDisposed()) return;

                ToolWindow toolWindow = ToolWindowManager.getInstance(project)
                        .getToolWindow("Stacktale");

                if (toolWindow != null) {
                    toolWindow.activate(null);
                }
            };
        }

        @Override
        public void reportsChanged(
                @Nullable Path log,
                @NotNull List<StReport> reports
        ) {
            currentLog = log;
            reportCount = reports.size();

            StatusBar installedStatusBar = statusBar;
            if (installedStatusBar != null) {
                installedStatusBar.updateWidget(WIDGET_ID);
            }
        }

        @Override
        public void dispose() {
            statusBar = null;
        }
    }
}
