package com.github.procyonprojects.marker.model;

import java.util.List;

public class Argument {
    private String name;
    private String description;
    private String type;
    private boolean optional;
    private List<String> values;

    public Argument(String name, String description, String type, boolean optional, List<String> values) {
        this.name = name;
        this.description = description;
        this.type = type;
        this.optional = optional;
        this.values = values;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isOptional() {
        return optional;
    }

    public void setOptional(boolean optional) {
        this.optional = optional;
    }

    public List<String> getValues() {
        return values;
    }

    public void setValues(List<String> values) {
        this.values = values;
    }
}
