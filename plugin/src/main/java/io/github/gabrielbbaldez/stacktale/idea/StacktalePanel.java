package io.github.gabrielbbaldez.stacktale.idea;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.ui.JBSplitter;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JTextArea;
import java.awt.Component;
import java.awt.Font;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.nio.file.Path;
import java.util.List;

/**
 * The Stacktale tool window: newest reports on the left, the raw block on the right,
 * and a toolbar to refresh / jump to the culprit / copy for an AI. Reports come from
 * the single project-level poll owned by {@link StacktaleReportService}.
 */
class StacktalePanel extends SimpleToolWindowPanel {

    private final Project project;
    private final ToolWindow toolWindow;
    private final StacktaleReportService reportService;
    private final DefaultListModel<StReport> model = new DefaultListModel<>();
    private final JBList<StReport> list = new JBList<>(model);
    private final JTextArea detail = new JTextArea();

    StacktalePanel(Project project, ToolWindow toolWindow) {
        super(true, true);
        this.project = project;
        this.toolWindow = toolWindow;
        this.reportService = StacktaleReportService.getInstance(project);

        list.setCellRenderer(new ReportCellRenderer());
        list.addListSelectionListener(e -> showSelected());
        list.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) jumpToCulprit();
            }
        });

        detail.setEditable(false);
        detail.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

        JBSplitter splitter = new JBSplitter(false, 0.38f);
        splitter.setFirstComponent(new JBScrollPane(list));
        splitter.setSecondComponent(new JBScrollPane(detail));
        setContent(splitter);
        setToolbar(buildToolbar().getComponent());

        reportService.addListener(this::reportsChanged);
    }

    private ActionToolbar buildToolbar() {
        DefaultActionGroup group = new DefaultActionGroup();
        group.add(new AnAction("Refresh", "Re-read errors-ai.log", AllIcons.Actions.Refresh) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                reportService.refreshNow();
            }
        });
        group.add(new AnAction("Jump to Culprit", "Open the culprit frame in the editor", AllIcons.Actions.EditSource) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                jumpToCulprit();
            }
        });
        group.add(new AnAction("Copy Report for AI", "Copy the selected report to the clipboard", AllIcons.Actions.Copy) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                copySelected();
            }
        });
        ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar("Stacktale", group, true);
        toolbar.setTargetComponent(this);
        return toolbar;
    }

    private void reportsChanged(@Nullable Path log, @NotNull List<StReport> reports) {
        if (log == null) {
            toolWindow.setTitle("Stacktale");
            model.clear();
            detail.setText("No errors-ai.log found in this project yet.\n\n"
                    + "Add the stacktale library and trigger an error — reports will appear here.");
            return;
        }

        toolWindow.setTitle("Stacktale — " + log);

        StReport previouslySelected = list.getSelectedValue();
        model.clear();
        for (int i = reports.size() - 1; i >= 0; i--) model.addElement(reports.get(i)); // newest first
        if (!model.isEmpty()) {
            int keep = previouslySelected == null ? 0 : indexOfId(previouslySelected.id());
            list.setSelectedIndex(Math.max(0, keep));
        } else {
            detail.setText("No error reports yet — the file has none.\n\n"
                    + "Resolved log path:\n" + log);
            detail.setCaretPosition(0);
        }
    }

    private int indexOfId(String id) {
        for (int i = 0; i < model.size(); i++) {
            if (model.get(i).id().equals(id)) return i;
        }
        return 0;
    }

    private void showSelected() {
        StReport report = list.getSelectedValue();
        detail.setText(report == null ? "" : report.block());
        detail.setCaretPosition(0);
    }

    private void jumpToCulprit() {
        StReport report = list.getSelectedValue();
        if (report != null && !ReportNavigator.navigate(project, report.culprit())) {
            detail.setText("Report #" + report.id() + " has no source frame to open.");
            detail.setCaretPosition(0);
        }
    }

    private void copySelected() {
        StReport report = list.getSelectedValue();
        if (report != null) CopyPasteManager.getInstance().setContents(new StringSelection(report.block()));
    }
    private static class ReportCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> l, Object value, int i, boolean selected, boolean focus) {
            super.getListCellRendererComponent(l, value, i, selected, focus);
            if (value instanceof StReport report) {
                setText("<html><b>#" + report.id() + "</b>&nbsp;&nbsp;" + escape(report.headline()) + "</html>");
            }
            return this;
        }
        private static String escape(String s) {
            return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        }
    }
}
