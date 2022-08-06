package com.github.procyonprojects.marker.comment;

import com.intellij.psi.PsiElement;

import java.util.List;
import java.util.Optional;

public class Comment {

    private List<Line> lines;

    public Comment(List<Line> lines) {
        this.lines = lines;
    }

    public List<Line> getLines() {
        return lines;
    }

    protected void setLines(List<Line> lines) {
        this.lines = lines;
    }

    public Optional<Line> firstLine() {
        return line(0);
    }

    public Optional<Line> line(int index) {
        if (lines.size() <= index) {
            return Optional.empty();
        }

        return Optional.of(lines.get(index));
    }

    public static class Line {

        private PsiElement element;

        public Line(PsiElement element) {
            this.element = element;
        }

        public PsiElement getElement() {
            return element;
        }

        public String getText() {
            return element.getText().substring(2);
        }

        public int startOffset() {
            return element.getTextRange().getStartOffset() + 2;
        }

        public int endOffset() {
            return element.getTextRange().getEndOffset() + 2;
        }
    }
}
