package com.github.procyonprojects.marker.metadata.v1.schema;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.github.procyonprojects.marker.metadata.Type;
import com.github.procyonprojects.marker.metadata.TypeInfo;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MapSchema extends Schema {

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
    @JsonSubTypes(value = {
            @JsonSubTypes.Type(value = StringSchema.class, name = "string"),
            @JsonSubTypes.Type(value = IntegerSchema.class, name = "integer"),
            @JsonSubTypes.Type(value = BoolSchema.class, name = "boolean"),
            @JsonSubTypes.Type(value = SliceSchema.class, name = "slice"),
            @JsonSubTypes.Type(value = MapSchema.class, name = "map"),
            @JsonSubTypes.Type(value = AnySchema.class, name = "any"),
    })
    @JsonProperty("values")
    private Schema valueSchema;

    public MapSchema() {
        super(new TypeInfo(Type.MapType));
    }

    public MapSchema(Schema valueSchema) {
        super(new TypeInfo(Type.MapType, valueSchema.getType()));
        this.valueSchema = valueSchema;
    }

    public Schema getValueSchema() {
        return valueSchema;
    }

    public void setValueSchema(Schema valueSchema) {
        this.valueSchema = valueSchema;
    }

    @Override
    public String getPresentableText() {
        return String.format("map[string]%s", valueSchema.getPresentableText());
    }
}
