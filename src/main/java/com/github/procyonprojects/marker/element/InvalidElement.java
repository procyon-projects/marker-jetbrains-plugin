package com.github.procyonprojects.marker.element;

import com.intellij.openapi.util.TextRange;

public class InvalidElement extends Element {

    private String message;

    public InvalidElement(String message, String text, TextRange range) {
        super(text, range);
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
