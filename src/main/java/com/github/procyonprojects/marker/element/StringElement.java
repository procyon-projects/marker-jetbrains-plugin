package com.github.procyonprojects.marker.element;

import com.intellij.openapi.util.TextRange;

import java.util.List;
import java.util.stream.Collectors;


public class StringElement extends Element {

    private List<Element> parts;

    public StringElement(List<Element> parts) {
        super(parts.stream().map(Element::getText).collect(Collectors.joining("")), null);
        this.parts = parts;
    }

    public List<Element> getParts() {
        return parts;
    }

    public void setParts(List<Element> parts) {
        this.parts = parts;
    }

    @Override
    public TextRange getRange() {
        if (parts.size() == 1) {
            return parts.get(0).getRange();
        }

        int startOffset = parts.get(0).getRange().getStartOffset();
        int endOffset = parts.get(parts.size() - 1).getRange().getEndOffset();
        return new TextRange(startOffset, endOffset);
    }
}
