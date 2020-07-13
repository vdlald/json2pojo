package com.vladislav.json2pojo;

public enum JsonArrayType {
    MIXED,
    STRING,
    NUMBER,
    BOOLEAN,
    OBJECT,
    ARRAY;

    public static JsonArrayType of(JsonPrimitiveType type) {
        switch (type) {
            case NUMBER:
                return NUMBER;
            case BOOLEAN:
                return BOOLEAN;
            default:
                return STRING;
        }
    }
}
