package com.github.procyonprojects.marker.provider;

import com.goide.project.GoSyntheticLibrary;
import com.goide.sdk.GoSdkUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.AdditionalLibraryRootsProvider;
import com.intellij.openapi.roots.SyntheticLibrary;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.*;

public class MarkerRootsProvider extends AdditionalLibraryRootsProvider {

    @Override
    public @NotNull Collection<SyntheticLibrary> getAdditionalProjectLibraries(@NotNull Project project) {
        Collection<SyntheticLibrary> libraries = new HashSet<>();
        libraries.add(new MarkerRootsProvider.MarkerProcessor(getMarkerRoots(project)));
        return libraries;
    }

    @Override
    public @NotNull Collection<VirtualFile> getRootsToWatch(@NotNull Project project) {
        return getMarkerRoots(project);
    }

    private Collection<VirtualFile> getMarkerRoots(@NotNull Project project) {
        Collection<VirtualFile> libraries = new HashSet<>();
        Optional<Module> module = GoSdkUtil.getGoModules(project).stream().findFirst();

        if (module.isPresent()) {
            VirtualFile firstGoPath = ContainerUtil.getFirstItem(GoSdkUtil.getGoPathRoots(project, module.get()));
            if (firstGoPath != null) {
                File file = new File(VfsUtilCore.virtualToIoFile(firstGoPath), "marker/pkg");
                VirtualFile vgo = VfsUtil.findFileByIoFile(file, false);
                if (vgo != null) {
                    libraries.add(vgo);
                    return libraries;
                }

                ApplicationManager.getApplication().invokeLater(() -> {
                    LocalFileSystem.getInstance().refreshIoFiles(Collections.singleton(file), true, false, null);
                });
            }
        }
        return libraries;
    }

    private static final class MarkerProcessor extends GoSyntheticLibrary {

        private final @NotNull Collection<VirtualFile> myRoots;

        private MarkerProcessor(@NotNull Collection<VirtualFile> roots) {
            super();
            this.myRoots = ContainerUtil.filter(roots, VirtualFile::isValid);
        }

        @Override
        public @NotNull Collection<VirtualFile> getSourceRoots() {
            return myRoots;
        }

        @Override
        public @Nullable Condition<VirtualFile> getExcludeFileCondition() {
            return (f) -> {
                if (f.isDirectory()) {
                    return false;
                } else {
                    return !"marker.processors.yaml".contentEquals(f.getNameSequence());
                }
            };
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            } else if (o != null && this.getClass() == o.getClass()) {
                MarkerRootsProvider.MarkerProcessor library = (MarkerProcessor) o;
                return this.myRoots.equals(library.myRoots);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.myRoots);
        }
    }
}
