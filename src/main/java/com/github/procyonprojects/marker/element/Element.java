package com.github.procyonprojects.marker.element;

import com.intellij.openapi.util.TextRange;

public class Element {
    private String text;
    private Element parent;
    private Element previous;
    private Element next;
    private TextRange range;

    public Element() {

    }

    public Element(String text, TextRange textRange) {
        this.text = text;
        this.range = textRange;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Element getParent() {
        return parent;
    }

    public void setParent(Element parent) {
        this.parent = parent;
    }

    public Element getPrevious() {
        return previous;
    }

    public void setPrevious(Element previous) {
        this.previous = previous;
    }

    public Element getNext() {
        return next;
    }

    public void setNext(Element next) {
        this.next = next;
    }

    public TextRange getRange() {
        return range;
    }

    public void setRange(TextRange range) {
        this.range = range;
    }
}
