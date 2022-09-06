package com.github.procyonprojects.marker.highlighter;

import com.github.procyonprojects.marker.element.Element;

public class MarkerProblem {

    private Element element;
    private String text;

    public MarkerProblem(Element element) {
        this.element = element;
    }

    public MarkerProblem(Element element, String text) {
        this.element = element;
        this.text = text;
    }

    public Element getElement() {
        return element;
    }

    public void setElement(Element element) {
        this.element = element;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
