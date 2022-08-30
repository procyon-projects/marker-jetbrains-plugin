package com.github.procyonprojects.marker.fix;

import com.intellij.codeInsight.intention.impl.BaseIntentionAction;
import com.intellij.codeInspection.util.IntentionFamilyName;
import com.intellij.codeInspection.util.IntentionName;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;

import java.util.List;

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

    }
}
