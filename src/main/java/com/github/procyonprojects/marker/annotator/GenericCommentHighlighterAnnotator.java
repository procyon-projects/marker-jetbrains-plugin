package com.github.procyonprojects.marker.annotator;

import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiPlainText;
import org.jetbrains.annotations.NotNull;

public class GenericCommentHighlighterAnnotator extends AbstractCommentHighlighterAnnotator {

    @Override
    protected boolean isCommentHighlightingElement(@NotNull PsiElement element) {
        return isCommentType(element) || isPlainTextHighlight(element);
    }

    private boolean isPlainTextHighlight(@NotNull PsiElement element) {
        return element instanceof PsiPlainText;
    }

    private boolean isCommentType(@NotNull PsiElement element) {
        return element instanceof PsiComment;
    }
}