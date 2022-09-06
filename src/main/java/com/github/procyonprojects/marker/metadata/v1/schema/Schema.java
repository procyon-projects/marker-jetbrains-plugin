package com.github.procyonprojects.marker.metadata.v1.schema;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.procyonprojects.marker.metadata.TypeInfo;

@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class Schema {

    private TypeInfo type;

    public Schema(TypeInfo type) {
        this.type = type;
    }

    public TypeInfo getType() {
        return type;
    }

    public void setType(TypeInfo type) {
        this.type = type;
    }

    public String getPresentableText() {
        return type.getPresentableText();
    }
}
