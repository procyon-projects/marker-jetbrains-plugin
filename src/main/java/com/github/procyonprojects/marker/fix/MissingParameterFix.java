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

public class MissingParameterFix extends BaseIntentionAction {

    private final List<String> missingParameters;

    public MissingParameterFix(List<String> missingParameters) {
        this.missingParameters = missingParameters;
    }

    @Override
    public @IntentionName @NotNull String getText() {
        return "Add missing parameters " + missingParameters;
    }

    @Override
    public @NotNull @IntentionFamilyName String getFamilyName() {
        return "Add missing parameters";
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
        return true;
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {

    }
}
