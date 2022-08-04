package com.github.procyonprojects.marker.metadata.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.github.procyonprojects.marker.metadata.Type;
import com.github.procyonprojects.marker.metadata.TypeInfo;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TypeInfoDeserializer extends JsonDeserializer<TypeInfo> {
    @Override
    public TypeInfo deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
        String type = jsonParser.getText();
        return getTypeInfo(type);
    }

    private TypeInfo getTypeInfo(String type) {
        if (StringUtils.isEmpty(type)) {
            return TypeInfo.INVALID_TYPE_INFO;
        }

        switch (type) {
            case "uint8":
            case "uint16":
            case "uint32":
            case "uint64":
            case "uint":
                return new TypeInfo(Type.UnsignedIntegerType);
            case "int8":
            case "int16":
            case "int32":
            case "int64":
            case "int":
                return new TypeInfo(Type.SignedIntegerType);
            case "bool":
                return new TypeInfo(Type.BooleanType);
            case "string":
                return new TypeInfo(Type.StringType);
            case "any":
                return new TypeInfo(Type.AnyType);
            case "raw":
                return new TypeInfo(Type.RawType);
            default:
                if (StringUtils.deleteWhitespace(type).matches("map\\[(.+)\\](.+)")) {
                    Pattern pattern = Pattern.compile("map\\[(.+)\\](.+)");
                    Matcher matcher = pattern.matcher(type);

                    if (matcher.find()) {
                        String key = matcher.group(1);
                        if (!"string".equals(key)) {
                            return TypeInfo.INVALID_TYPE_INFO;
                        }

                        TypeInfo value = getTypeInfo(matcher.group(2));
                        if (value == TypeInfo.INVALID_TYPE_INFO) {
                            return TypeInfo.INVALID_TYPE_INFO;
                        }

                        return new TypeInfo(Type.MapType, value);
                    }

                    return TypeInfo.INVALID_TYPE_INFO;
                } else if (StringUtils.deleteWhitespace(type).matches("\\[\\](.+)")) {
                    Pattern pattern = Pattern.compile("map\\[(.+)\\](.+)");
                    Matcher matcher = pattern.matcher(type);

                    if (matcher.find()) {
                        TypeInfo item = getTypeInfo(matcher.group(1));

                        if (item == TypeInfo.INVALID_TYPE_INFO) {
                            return TypeInfo.INVALID_TYPE_INFO;
                        }

                        return new TypeInfo(Type.SliceType, item);
                    }
                }
        }

        return TypeInfo.INVALID_TYPE_INFO;
    }
}
