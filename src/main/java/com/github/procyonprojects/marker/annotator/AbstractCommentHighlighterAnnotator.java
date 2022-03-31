package com.github.procyonprojects.marker.annotator;

import com.github.procyonprojects.marker.highlighter.CommentHighlighter;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiWhiteSpace;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public abstract class AbstractCommentHighlighterAnnotator implements Annotator {

    private static final CommentHighlighter commentHighlighter = ApplicationManager.getApplication().getService(CommentHighlighter.class);

    @Override
    public void annotate(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
        if (isCommentHighlightingElement(element)) {
            final String comment = extractCommentTextFromElement(element);
            int startOffset = element.getTextRange().getStartOffset();

            int commentIndex = 0;
            boolean seenWhiteSpace = false;

            PsiElement goElement = element.getNextSibling();
            while (goElement != null) {
                if (commentIndex > 1) {
                    break;
                }

                if (goElement instanceof PsiWhiteSpace && !goElement.getText().equals("\n")) {
                    seenWhiteSpace = true;
                } else if(seenWhiteSpace && goElement instanceof PsiComment) {
                    seenWhiteSpace = false;
                    commentIndex++;
                } else if (!(goElement instanceof PsiWhiteSpace || goElement instanceof PsiComment)) {
                    break;
                }
                goElement = goElement.getNextSibling();
            }

            if (commentIndex > 1) {
                return;
            }

            List<Pair<TextRange, TextAttributesKey>> highlights = commentHighlighter.getHighlights(comment, startOffset);

            for (Pair<TextRange, TextAttributesKey> highlight : highlights) {
                holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                        .range(highlight.first)
                        .textAttributes(highlight.second)
                        .create();
            }
        }
    }

    protected String extractCommentTextFromElement(@NotNull PsiElement element) {
        return element.getText();
    }

    protected abstract boolean isCommentHighlightingElement(@NotNull PsiElement element);

}
