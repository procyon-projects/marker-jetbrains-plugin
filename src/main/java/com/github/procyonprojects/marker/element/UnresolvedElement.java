package com.github.procyonprojects.marker.element;

import com.intellij.openapi.util.TextRange;

public class UnresolvedElement extends Element {

    private String message;

    public UnresolvedElement(String message, String text, TextRange textRange) {
        super(text, textRange);
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
