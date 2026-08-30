package io.github.gabrielbbaldez.stacktale.idea;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.project.Project;
import com.intellij.ui.JBIntSpinner;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JPanel;

/** Settings &rarr; Tools &rarr; Stacktale: point the tool window at a log file and tune the poll. */
public final class StacktaleConfigurable implements Configurable {

    private final Project project;

    private JBTextField logPathField;
    private JBIntSpinner pollSpinner;
    private JPanel panel;

    public StacktaleConfigurable(@NotNull Project project) {
        this.project = project;
    }

    @Override
    public @Nls(capitalization = Nls.Capitalization.Title) String getDisplayName() {
        return "Stacktale";
    }

    @Override
    public @Nullable JComponent createComponent() {
        logPathField = new JBTextField();
        pollSpinner = new JBIntSpinner(
                StSettings.DEFAULT_POLL_SECONDS,
                StSettings.MIN_POLL_SECONDS,
                StSettings.MAX_POLL_SECONDS);

        panel = FormBuilder.createFormBuilder()
                .addLabeledComponent(new JBLabel("Log file:"), logPathField, 1, false)
                .addTooltip("Absolute, or relative to the project root. "
                        + "Leave empty to auto-detect errors-ai.log.")
                .addLabeledComponent(new JBLabel("Poll interval (seconds):"), pollSpinner, 1, false)
                .addComponentFillVertically(new JPanel(), 0)
                .getPanel();

        reset();
        return panel;
    }

    @Override
    public boolean isModified() {
        StacktaleSettings settings = StacktaleSettings.getInstance(project);
        return !settings.logPath().equals(logPath()) || settings.pollSeconds() != pollSeconds();
    }

    @Override
    public void apply() {
        StacktaleSettings settings = StacktaleSettings.getInstance(project);
        settings.setLogPath(logPath());
        settings.setPollSeconds(pollSeconds());

        // The next tick would pick both up on its own; re-reading now means a changed path
        // shows its reports as soon as the dialog closes.
        StacktaleReportService.getInstance(project).refreshNow();
    }

    @Override
    public void reset() {
        StacktaleSettings settings = StacktaleSettings.getInstance(project);
        logPathField.setText(settings.logPath());
        pollSpinner.setNumber(settings.pollSeconds());
    }

    @Override
    public void disposeUIResources() {
        panel = null;
        logPathField = null;
        pollSpinner = null;
    }

    private String logPath() {
        return StSettings.normalizeLogPath(logPathField.getText());
    }

    /** The spinner already holds the value in range, so the settings class has nothing to clamp. */
    private int pollSeconds() {
        return pollSpinner.getNumber();
    }
}
