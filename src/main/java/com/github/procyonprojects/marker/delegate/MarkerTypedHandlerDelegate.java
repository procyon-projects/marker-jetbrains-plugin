package com.github.procyonprojects.marker.delegate;

import com.intellij.codeInsight.AutoPopupController;
import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.codeInsight.editorActions.TypedHandlerDelegate;
import com.intellij.lang.injection.InjectedLanguageManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Condition;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

public class MarkerTypedHandlerDelegate extends TypedHandlerDelegate {

    @NotNull
    @Override
    public Result checkAutoPopup(char charTyped, @NotNull final Project project, @NotNull final Editor editor, @NotNull PsiFile file) {
        PsiFile topLevelFile = InjectedLanguageManager.getInstance(project).getTopLevelFile(file);
        if (charTyped == '+' || charTyped == ':') {
            autoPopupParameter(project, editor);
        }
        return super.checkAutoPopup(charTyped, project, editor, file);
    }


    @NotNull
    @Override
    public Result charTyped(char c, @NotNull Project project, @NotNull Editor editor, @NotNull PsiFile file) {
        return super.charTyped(c, project, editor, file);
    }

    @NotNull
    @Override
    public Result beforeCharTyped(char c, @NotNull Project project, @NotNull Editor editor, @NotNull PsiFile file,
                                  @NotNull FileType fileType) {
        return super.beforeCharTyped(c, project, editor, file, fileType);
    }

    private static void autoPopupParameter(final Project project, final Editor editor) {
        AutoPopupController.getInstance(project).autoPopupMemberLookup(editor, CompletionType.BASIC,
                new Condition<PsiFile>() {
                    @Override
                    public boolean value(PsiFile psiFile) {
                        return true;
                    }
                });
    }


}