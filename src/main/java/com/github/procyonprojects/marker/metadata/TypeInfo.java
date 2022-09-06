package com.github.procyonprojects.marker.metadata;

import org.apache.commons.lang3.StringUtils;

public class TypeInfo {

    public static final TypeInfo INVALID_TYPE_INFO = new TypeInfo(Type.InvalidType);
    public static final TypeInfo ANY_TYPE_INFO = new TypeInfo(Type.AnyType);

    private final String presentableText;
    private final Type actualType;
    private final TypeInfo itemType;

    public TypeInfo(Type actualType) {
        this(actualType, (TypeInfo) null);
    }

    public TypeInfo(Type actualType, String presentableText) {
        this(actualType, null, presentableText);
    }

    public TypeInfo(Type actualType, TypeInfo itemType) {
        this(actualType, itemType, null);
    }

    public TypeInfo(Type actualType, TypeInfo itemType, String presentableText) {
        this.actualType = actualType;
        this.itemType = itemType;
        this.presentableText = presentableText;
    }

    public Type getActualType() {
        return actualType;
    }

    public TypeInfo getItemType() {
        return itemType;
    }

    public String getPresentableText() {
        if (StringUtils.isEmpty(presentableText)) {
            return getTypeText(getActualType(), getItemType());
        }

        return presentableText;
    }

    private String getTypeText(Type type, TypeInfo itemType) {
        switch (type) {
            case UnsignedIntegerType:
                return "uint";
            case SignedIntegerType:
                return "int";
            case BooleanType:
                return "bool";
            case StringType:
                return "string";
            case AnyType:
                return "any";
            case RawType:
                return "raw";
            case GoFunction:
                return "func";
            case GoType:
                return "type";
            case MapType:
                return String.format("map[string]%s", getTypeText(itemType.getActualType(), itemType.getItemType()));
            case SliceType:
                return String.format("[]%s", getTypeText(itemType.getActualType(), itemType.getItemType()));
            default:
                return "invalid";
        }
    }
}
