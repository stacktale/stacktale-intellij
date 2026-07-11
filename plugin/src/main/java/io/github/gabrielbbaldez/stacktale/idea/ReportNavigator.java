package io.github.gabrielbbaldez.stacktale.idea;

import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;

import java.util.Collection;

/** Opens the source location a report points at (its culprit / stack frame) in the editor. */
final class ReportNavigator {

    private ReportNavigator() {
    }

    static boolean navigate(Project project, StFrame frame) {
        if (frame == null) return false;
        // index queries need a read action; the file lives by simple name in the report
        Collection<VirtualFile> files = ReadAction.compute(() ->
                FilenameIndex.getVirtualFilesByName(frame.fileName(), GlobalSearchScope.projectScope(project)));
        if (files.isEmpty()) return false;
        VirtualFile target = files.iterator().next();
        // OpenFileDescriptor lines are 0-based; report lines are 1-based
        new OpenFileDescriptor(project, target, Math.max(0, frame.line() - 1), 0).navigate(true);
        return true;
    }
}
