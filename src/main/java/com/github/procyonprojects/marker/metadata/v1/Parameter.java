package com.github.procyonprojects.marker.metadata.v1;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.github.procyonprojects.marker.metadata.TypeInfo;
import com.github.procyonprojects.marker.metadata.v1.schema.*;
import com.intellij.util.ObjectUtils;
import org.apache.commons.collections.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


@JsonIgnoreProperties(ignoreUnknown = true)
public class Parameter {

    @JsonProperty(required = true)
    private String name;
    @JsonProperty(required = true)
    private String description;
    private boolean required;
    @JsonProperty("default")
    private String defaultValue;
    @JsonProperty("enum")
    private List<EnumValue> enumValues;
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
    @JsonSubTypes(value = {
            @JsonSubTypes.Type(value = StringSchema.class, name = "string"),
            @JsonSubTypes.Type(value = IntegerSchema.class, name = "integer"),
            @JsonSubTypes.Type(value = BoolSchema.class, name = "boolean"),
            @JsonSubTypes.Type(value = SliceSchema.class, name = "slice"),
            @JsonSubTypes.Type(value = MapSchema.class, name = "map"),
            @JsonSubTypes.Type(value = AnySchema.class, name = "any"),
            @JsonSubTypes.Type(value = TypeSchema.class, name = "type"),
            @JsonSubTypes.Type(value = FunctionSchema.class, name = "func"),
    })
    private Schema schema;

    public Parameter() {

    }

    public Parameter(String name, Schema schema, String description) {
        this(name, schema, description, false);
    }

    public Parameter(String name, Schema schema, String description, boolean isRequired) {
        this(name, schema, description, isRequired, "");
    }

    public Parameter(String name, Schema schema, String description, String defaultValue) {
        this(name, schema, description, false, defaultValue, new ArrayList<>());
    }

    public Parameter(String name, Schema schema, String description, boolean isRequired, String defaultValue) {
        this(name, schema, description, isRequired, defaultValue, new ArrayList<>());
    }

    public Parameter(String name, Schema schema, String description, boolean isRequired, List<EnumValue> enumValues) {
        this(name, schema, description, isRequired, null, enumValues);
    }

    public Parameter(String name, Schema schema, String description, List<EnumValue> enumValues) {
        this(name, schema, description, false, null, enumValues);
    }

    public Parameter(String name, Schema schema, String description, boolean isRequired, String defaultValue, List<EnumValue> enumValues) {
        this.name = name;
        this.description = description;
        this.required = isRequired;
        this.defaultValue = defaultValue;
        this.enumValues = enumValues;
        this.schema = schema;
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

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public List<EnumValue> getEnumValues() {
        if (CollectionUtils.isEmpty(enumValues)) {
            return Collections.emptyList();
        }
        return enumValues;
    }

    public void setEnumValues(List<EnumValue> enumValues) {
        this.enumValues = enumValues;
    }

    public Schema getSchema() {
        return schema;
    }

    public void setSchema(Schema schema) {
        this.schema = schema;
    }

    public TypeInfo getType() {
        if (schema == null) {
            return TypeInfo.INVALID_TYPE_INFO;
        }

        return schema.getType();
    }
}
