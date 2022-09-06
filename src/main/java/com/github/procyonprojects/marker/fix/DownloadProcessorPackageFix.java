package com.github.procyonprojects.marker.fix;

import com.goide.sdk.GoSdk;
import com.goide.sdk.GoSdkService;
import com.goide.sdk.GoSdkUtil;
import com.goide.util.GoExecutor;
import com.intellij.codeInsight.intention.impl.BaseIntentionAction;
import com.intellij.codeInspection.util.IntentionFamilyName;
import com.intellij.codeInspection.util.IntentionName;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.psi.PsiFile;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

public class DownloadProcessorPackageFix extends BaseIntentionAction {

    private final String pkg;

    public DownloadProcessorPackageFix(String pkg) {
        this.pkg = pkg;
    }

    @Override
    public @IntentionName @NotNull String getText() {
        return String.format("Download '%s'", pkg);
    }

    @Override
    public @NotNull @IntentionFamilyName String getFamilyName() {
        return "Download processor package";
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
        return true;
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
        Optional<Module> module = GoSdkUtil.getGoModules(project).stream().findFirst();
        if (module.isEmpty()) {
            return;
        }

        GoSdk sdk = GoSdkService.getInstance(project).getSdk(module.get());
        if (sdk.isValid()) {
            CommandProcessor.getInstance().runUndoTransparentAction(() -> {
                GoExecutor executor = GoExecutor.in(project, module.get()).disablePty()
                        .withPresentableName(String.format("marker download %s", pkg))
                        .withParameters("marker", "download", pkg)
                        .withPrintingOutputAsStatus()
                        .showNotifications(false, true);
                executor.executeWithProgress(true, true, executionResult -> {
                    VirtualFileManager.getInstance().asyncRefresh(null);
                });
            });
        }
    }
}
