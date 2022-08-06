package com.github.procyonprojects.marker.annotator;

import com.github.procyonprojects.marker.Utils;
import com.github.procyonprojects.marker.comment.Comment;
import com.github.procyonprojects.marker.highlighter.CommentHighlighter;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

@Service
public class CommentAnnotator implements Annotator {

    private static final CommentHighlighter commentHighlighter = ApplicationManager.getApplication().getService(CommentHighlighter.class);

    @Override
    public void annotate(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
        Optional<Comment> comment = Utils.getMarkerComment(element);
        comment.ifPresent(value -> commentHighlighter.highlight(value, element, holder));

        comment = Utils.findMarkerCommentFirstLine(element);
        comment.ifPresent(value -> commentHighlighter.highlight(value, element, holder));
    }

}
