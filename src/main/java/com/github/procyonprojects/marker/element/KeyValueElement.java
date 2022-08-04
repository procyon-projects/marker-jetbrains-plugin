package com.github.procyonprojects.marker.element;

public class KeyValueElement extends Element {

    private Element keyElement;
    private Element colonElement;
    private Element valueElement;

    public Element getKeyElement() {
        return keyElement;
    }

    public void setKeyElement(Element keyElement) {
        this.keyElement = keyElement;
    }

    public Element getColonElement() {
        return colonElement;
    }

    public void setColonElement(Element colonElement) {
        this.colonElement = colonElement;
    }

    public Element getValueElement() {
        return valueElement;
    }

    public void setValueElement(Element valueElement) {
        this.valueElement = valueElement;
    }
}
