package com.github.procyonprojects.marker.metadata;

public class Enum {
    private Object value;
    private String description;

    public Enum(Object value) {
        this(value, "");
    }

    public Enum(Object value, String description) {
        this.value = value;
        this.description = description;
    }

    public Object getValue() {
        return value;
    }

    protected void setValue(Object value) {
        this.value = value;
    }

    public String getDescription() {
        return description;
    }

    protected void setDescription(String description) {
        this.description = description;
    }
}
