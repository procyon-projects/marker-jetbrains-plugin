package com.github.procyonprojects.marker.metadata.v1.schema;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.procyonprojects.marker.metadata.Type;
import com.github.procyonprojects.marker.metadata.TypeInfo;

@JsonIgnoreProperties(ignoreUnknown = true)
public class FunctionSchema extends Schema {

    public FunctionSchema() {
        super(new TypeInfo(Type.GoFunction));
    }

}
