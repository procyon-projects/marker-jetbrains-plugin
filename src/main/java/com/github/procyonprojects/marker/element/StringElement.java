package com.github.procyonprojects.marker.element;

import java.util.List;


public class StringElement extends Element {

    private List<Element> parts;

    public StringElement(List<Element> parts) {
        this.parts = parts;
    }

    public List<Element> getParts() {
        return parts;
    }

    public void setParts(List<Element> parts) {
        this.parts = parts;
    }
}
