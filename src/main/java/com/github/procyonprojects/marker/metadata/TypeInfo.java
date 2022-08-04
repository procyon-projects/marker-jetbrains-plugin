package com.github.procyonprojects.marker.metadata;

public class TypeInfo {

    public static final TypeInfo INVALID_TYPE_INFO = new TypeInfo(Type.InvalidType);
    public static final TypeInfo ANY_TYPE_INFO = new TypeInfo(Type.AnyType);

    private final Type actualType;
    private final TypeInfo itemType;

    public TypeInfo(Type actualType) {
        this(actualType, null);
    }

    public TypeInfo(Type actualType, TypeInfo itemType) {
        this.actualType = actualType;
        this.itemType = itemType;
    }

    public Type getActualType() {
        return actualType;
    }

    public TypeInfo getItemType() {
        return itemType;
    }
}
