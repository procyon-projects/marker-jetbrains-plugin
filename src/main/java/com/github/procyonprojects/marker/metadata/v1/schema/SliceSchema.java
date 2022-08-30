package com.github.procyonprojects.marker.metadata.v1.schema;

import b.h.T;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.github.procyonprojects.marker.metadata.Type;
import com.github.procyonprojects.marker.metadata.TypeInfo;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SliceSchema extends Schema {

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
    @JsonSubTypes(value = {
            @JsonSubTypes.Type(value = StringSchema.class, name = "string"),
            @JsonSubTypes.Type(value = IntegerSchema.class, name = "integer")
    })
    @JsonProperty("items")
    private Schema itemSchema;

    public SliceSchema() {
        super(new TypeInfo(Type.SliceType));
    }

    public SliceSchema(Schema itemSchema) {
        super(new TypeInfo(Type.SliceType, itemSchema.getType()));
        this.itemSchema = itemSchema;
    }

    public Schema getItemSchema() {
        return itemSchema;
    }

    public void setItemSchema(Schema itemSchema) {
        this.itemSchema = itemSchema;
    }

    @Override
    public String getPresentableText() {
        return String.format("[]%s", itemSchema.getPresentableText());
    }
}
