package com.github.procyonprojects.marker.element;

import java.util.ArrayList;
import java.util.List;

public class MapElement extends Element {

    private Element leftBrace;
    private Element rightBrace;

    public Element getLeftBrace() {
        return leftBrace;
    }

    public void setLeftBrace(Element leftBrace) {
        this.leftBrace = leftBrace;
    }

    public Element getRightBrace() {
        return rightBrace;
    }

    public void setRightBrace(Element rightBrace) {
        this.rightBrace = rightBrace;
    }

    public List<Element> getItems() {
        final List<Element> items = new ArrayList<>();
        Element current = getNext();
        while (current != null) {
            items.add(current);
            current = current.getNext();
        }
        return items;
    }
}
