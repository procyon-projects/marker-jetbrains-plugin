package com.github.procyonprojects.marker.scope;

import com.github.procyonprojects.marker.lang.MarkerProcessorsType;
import com.intellij.openapi.fileTypes.FileTypeRegistry;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;

public class MarkerScope extends GlobalSearchScope {

    public MarkerScope(Project project) {
        super(project);
    }

    @Override
    public boolean isSearchInModuleContent(@NotNull Module aModule) {
        return false;
    }

    @Override
    public boolean isSearchInLibraries() {
        return false;
    }

    @Override
    public boolean contains(@NotNull VirtualFile file) {
        return FileTypeRegistry.getInstance().isFileOfType(file, MarkerProcessorsType.INSTANCE);
    }
}
