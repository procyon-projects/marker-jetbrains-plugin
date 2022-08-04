package com.github.procyonprojects.marker.element;

import com.github.procyonprojects.marker.metadata.Parameter;

public class SliceElement extends Element {

    private Element leftBrace;
    private Element rightBrace;
    private Parameter parameter;

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

    public Parameter getParameter() {
        return parameter;
    }

    public void setParameter(Parameter parameter) {
        this.parameter = parameter;
    }
}
