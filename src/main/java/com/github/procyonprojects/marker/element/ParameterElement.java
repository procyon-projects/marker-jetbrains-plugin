package com.github.procyonprojects.marker.element;

import com.github.procyonprojects.marker.metadata.TypeInfo;
import com.github.procyonprojects.marker.metadata.v1.Parameter;
import com.intellij.openapi.util.TextRange;

public class ParameterElement extends Element {

    private Element name;
    private Element equalSign;
    private Element value;
    private TypeInfo typeInfo;
    private Parameter parameter;

    public Element getName() {
        return name;
    }

    public void setName(Element name) {
        this.name = name;
    }

    public Element getEqualSign() {
        return equalSign;
    }

    public void setEqualSign(Element equalSign) {
        this.equalSign = equalSign;
    }

    public Element getValue() {
        return value;
    }

    public void setValue(Element value) {
        this.value = value;
    }

    public TypeInfo getTypeInfo() {
        return typeInfo;
    }

    public void setTypeInfo(TypeInfo typeInfo) {
        this.typeInfo = typeInfo;
    }

    public Parameter getParameter() {
        return parameter;
    }

    public void setParameter(Parameter parameter) {
        this.parameter = parameter;
    }

    @Override
    public TextRange getRange() {
        int startOffset = -1;
        int endOffset = -1;

        if (name != null) {
            startOffset = name.getRange().getStartOffset();
            endOffset = name.getRange().getEndOffset();
        }

        if (equalSign != null) {
            endOffset = equalSign.getRange().getEndOffset();
            if (startOffset == -1) {
                startOffset = equalSign.getRange().getStartOffset();
            }
        }

        if (value != null) {
            endOffset = value.getRange().getEndOffset();
            if (startOffset == -1) {
                startOffset = value.getRange().getStartOffset();
            }
        }

        return new TextRange(startOffset, endOffset);
    }
}
