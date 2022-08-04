package com.github.procyonprojects.marker.element;

import com.github.procyonprojects.marker.metadata.TypeInfo;

public class ParameterElement extends Element {

    private Element name;
    private Element equalSign;
    private Element value;
    private TypeInfo typeInfo;

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
}
