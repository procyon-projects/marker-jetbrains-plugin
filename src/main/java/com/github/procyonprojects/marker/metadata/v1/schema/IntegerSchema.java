package com.github.procyonprojects.marker.metadata.v1.schema;

import b.h.T;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.procyonprojects.marker.metadata.Type;
import com.github.procyonprojects.marker.metadata.TypeInfo;

@JsonIgnoreProperties(ignoreUnknown = true)
public class IntegerSchema extends Schema {

    @JsonProperty(required = true)
    private String format;

    public IntegerSchema() {
        super(new TypeInfo(Type.SignedIntegerType));
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        switch (format) {
            case "int":
            case "int8":
            case "int16":
            case "int32":
            case "int64":
                setType(new TypeInfo(Type.SignedIntegerType));
            case "uint":
            case "uint8":
            case "uint16":
            case "uint32":
            case "uint64":
                setType(new TypeInfo(Type.UnsignedIntegerType));
        }
        this.format = format;
    }

    @Override
    public String getPresentableText() {
        return format;
    }
}
