package com.github.procyonprojects.marker.element;

import com.intellij.openapi.util.TextRange;

public class IntegerElement extends Element {

    private Integer value;

    public IntegerElement(Integer value, String text, TextRange range) {
        super(text, range);
        this.value = value;
    }

    public Integer getValue() {
        return this.value;
    }
}
