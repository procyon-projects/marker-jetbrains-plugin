package com.github.procyonprojects.marker.metadata.v1.schema;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.procyonprojects.marker.metadata.Type;
import com.github.procyonprojects.marker.metadata.TypeInfo;
import com.github.procyonprojects.marker.metadata.v1.EnumValue;
import org.apache.commons.collections.CollectionUtils;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class StringSchema extends Schema {

    @JsonProperty(required = true)
    private String format;
    private String pattern;
    @JsonProperty("enum")
    private List<EnumValue> enums;

    public StringSchema() {
        this(null);
    }

    public StringSchema(List<EnumValue> enums) {
        super(new TypeInfo(Type.StringType));
        this.enums = enums;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    public List<EnumValue> getEnums() {
        if (CollectionUtils.isEmpty(enums)) {
            return List.of();
        }
        return enums;
    }

    public void setEnums(List<EnumValue> enums) {
        this.enums = enums;
    }
}
