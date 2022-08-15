package com.github.procyonprojects.marker.metadata;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.github.procyonprojects.marker.metadata.deserializer.TypeInfoDeserializer;
import org.apache.commons.collections.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Parameter {

    private String name;
    @JsonDeserialize(using = TypeInfoDeserializer.class)
    private TypeInfo type;
    private String description;
    private boolean required;
    @JsonProperty("default")
    private Object defaultValue;
    @JsonProperty("enum")
    private List<Enum> enumValues;

    public Parameter() {

    }

    public Parameter(String name, TypeInfo typeInfo, String description) {
        this(name, typeInfo, description, false);
    }

    public Parameter(String name, TypeInfo typeInfo, String description, boolean isRequired) {
        this(name, typeInfo, description, isRequired, null);
    }

    public Parameter(String name, TypeInfo typeInfo, String description, Object defaultValue) {
        this(name, typeInfo, description, false, defaultValue, new ArrayList<>());
    }

    public Parameter(String name, TypeInfo typeInfo, String description, boolean isRequired, Object defaultValue) {
        this(name, typeInfo, description, isRequired, defaultValue, new ArrayList<>());
    }

    public Parameter(String name, TypeInfo typeInfo, String description, boolean isRequired, List<Enum> enumValues) {
        this(name, typeInfo, description, isRequired, null, enumValues);
    }

    public Parameter(String name, TypeInfo typeInfo, String description, List<Enum> enumValues) {
        this(name, typeInfo, description, false, null, enumValues);
    }

    public Parameter(String name, TypeInfo typeInfo, String description, boolean isRequired, Object defaultValue, List<Enum> enumValues) {
        this.name = name;
        this.type = typeInfo;
        this.description = description;
        this.required = isRequired;
        this.defaultValue = defaultValue;
        this.enumValues = enumValues;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public TypeInfo getType() {
        return type;
    }

    public void setType(TypeInfo type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public Object getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(Object defaultValue) {
        this.defaultValue = defaultValue;
    }

    public List<Enum> getEnumValues() {
        if (CollectionUtils.isEmpty(enumValues)) {
            return Collections.emptyList();
        }
        return enumValues;
    }

    public void setEnumValues(List<Enum> enumValues) {
        this.enumValues = enumValues;
    }
}
