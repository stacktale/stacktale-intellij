package io.github.gabrielbbaldez.stacktale.idea;

import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/** Opens the source location a report points at (its culprit / stack frame) in the editor. */
final class ReportNavigator {

    private ReportNavigator() {
    }

    static boolean navigate(Project project, StFrame frame) {
        if (frame == null) return false;

        // Index queries need a read action; the file lives by simple name in the report.
        Collection<VirtualFile> files = ReadAction.compute(() ->
                FilenameIndex.getVirtualFilesByName(frame.fileName(), GlobalSearchScope.projectScope(project)));

        if (files.isEmpty()) return false;

        List<VirtualFile> candidates = new ArrayList<>(files);
        candidates.sort(Comparator.comparing(VirtualFile::getPath));

        if (candidates.size() > 1) {
            JBPopupFactory.getInstance()
                    .createPopupChooserBuilder(candidates)
                    .setTitle("Choose Source File")
                    .setItemChosenCallback(target ->
                            openSource(project, target, frame.line()))
                    .createPopup()
                    .showCenteredInCurrentWindow(project);

            return true;
        }

        openSource(project, candidates.get(0), frame.line());
        return true;
    }

    private static void openSource(Project project, VirtualFile target, int line) {
        // OpenFileDescriptor lines are 0-based; report lines are 1-based.
        new OpenFileDescriptor(
                project,
                target,
                Math.max(0, line - 1),
                0
        ).navigate(true);
    }
}