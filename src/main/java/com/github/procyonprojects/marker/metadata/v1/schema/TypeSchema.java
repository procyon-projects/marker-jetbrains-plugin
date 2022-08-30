package com.github.procyonprojects.marker.metadata.v1.schema;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.procyonprojects.marker.metadata.Type;
import com.github.procyonprojects.marker.metadata.TypeInfo;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TypeSchema extends Schema {

    public TypeSchema() {
        super(new TypeInfo(Type.GoType));
    }

}
