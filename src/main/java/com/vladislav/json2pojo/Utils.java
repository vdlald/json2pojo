package com.vladislav.json2pojo;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import lombok.experimental.UtilityClass;

import java.util.*;
import java.util.stream.Collectors;

@UtilityClass
class Utils {

    public JsonElementType determineTypeOfJsonElement(JsonElement jsonElement) {
        if (jsonElement.isJsonPrimitive()) {
            return JsonElementType.PRIMITIVE;
        } else if (jsonElement.isJsonObject()) {
            return JsonElementType.OBJECT;
        } else if (jsonElement.isJsonArray()) {
            return JsonElementType.ARRAY;
        } else {
            return JsonElementType.NULL;
        }
    }

    public NumberType determineTypeOfNumber(Number number) {
        final int intValue = number.intValue();
        final long longValue = number.longValue();
        final double doubleValue = number.doubleValue();

        if ((double) intValue - doubleValue != 0) {
            return NumberType.DOUBLE;
        } else if (longValue - (long) intValue != 0) {
            return NumberType.LONG;
        } else {
            return NumberType.INT;
        }
    }

    public JsonPrimitiveType determineTypeOfJsonPrimitive(JsonPrimitive jsonPrimitive) {
        if (jsonPrimitive.isString()) {
            return JsonPrimitiveType.STRING;
        } else if (jsonPrimitive.isNumber()) {
            return JsonPrimitiveType.NUMBER;
        } else {
            return JsonPrimitiveType.BOOLEAN;
        }
    }

    public Pair<JsonElement, JsonArrayType> determineTypeOfJsonArray(JsonArray jsonArray) {
        List<JsonElement> jsonElements = new ArrayList<>();
        jsonArray.forEach(jsonElement -> {
            if (!jsonElement.isJsonNull()) {
                jsonElements.add(jsonElement);
            }
        });
        final Set<JsonElementType> jsonArrayTypes = jsonElements.stream()
                .map(Utils::determineTypeOfJsonElement)
                .collect(Collectors.toSet());
        if (jsonArrayTypes.size() == 1) {
            final JsonElementType jsonElementType = jsonArrayTypes.iterator().next();
            switch (jsonElementType) {
                case PRIMITIVE:
                    final Set<JsonPrimitiveType> jsonPrimitiveTypes = jsonElements.stream()
                            .map(JsonElement::getAsJsonPrimitive)
                            .map(Utils::determineTypeOfJsonPrimitive)
                            .collect(Collectors.toSet());
                    if (jsonPrimitiveTypes.size() == 1) {
                        switch (jsonPrimitiveTypes.iterator().next()) {
                            case BOOLEAN:
                                return Pair.of(null, JsonArrayType.BOOLEAN);
                            case NUMBER:
                                return Pair.of(null, JsonArrayType.NUMBER);
                            default:
                                return Pair.of(null, JsonArrayType.STRING);
                        }
                    } else {
                        return Pair.of(null, JsonArrayType.MIXED);
                    }
                case OBJECT:
                    final List<JsonObject> jsonObjects = jsonElements.stream()
                            .map(JsonElement::getAsJsonObject).collect(Collectors.toList());
                    final Set<String> allFields = jsonObjects.stream()
                            .map(JsonObject::entrySet)
                            .flatMap(Collection::stream)
                            .map(Map.Entry::getKey)
                            .collect(Collectors.toSet());
                    final JsonObject jsonObject = jsonObjects.get(0);
                    if (jsonObject.entrySet().size() == allFields.size()) {
                        return Pair.of(jsonObject, JsonArrayType.OBJECT);
                    } else {
                        return Pair.of(null, JsonArrayType.MIXED);
                    }
                default:  // is array
                    final List<JsonArray> jsonArrays = jsonElements.stream()
                            .map(JsonElement::getAsJsonArray)
                            .collect(Collectors.toList());
                    final List<Pair<JsonElement, JsonArrayType>> jsonArraysPair = jsonArrays.stream()
                            .map(Utils::determineTypeOfJsonArray).collect(Collectors.toList());
                    final Set<JsonArrayType> jsonArrayTypes1 = jsonArraysPair.stream().map(Pair::getValue2).collect(Collectors.toSet());
                    if (jsonArrayTypes1.size() == 1) {
                        final JsonArrayType next = jsonArrayTypes1.iterator().next();
                        // todo: implement
                    }
                    return Pair.of(null, JsonArrayType.MIXED);
            }
        } else {
            return Pair.of(null, JsonArrayType.MIXED);
        }
    }

    public String capitalize(String str) {
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    public String toCamelCase(String string, boolean firstWordToLowerCase) {
        boolean isPrevLowerCase = false, isNextUpperCase = !firstWordToLowerCase;
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < string.length(); i++) {
            char currentChar = string.charAt(i);
            if (!Character.isLetterOrDigit(currentChar)) {
                isNextUpperCase = result.length() > 0 || isNextUpperCase;
            } else {
                result.append(
                        isNextUpperCase ? Character.toUpperCase(currentChar) :
                                isPrevLowerCase ? currentChar : Character.toLowerCase(currentChar)
                );
                isNextUpperCase = false;
            }
            isPrevLowerCase = result.length() > 0 && Character.isLowerCase(currentChar);
        }
        return result.toString();
    }

    public boolean isValidFieldName(String fieldName) {
        return fieldName.matches("[a-z][a-zA-Z0-9]*");
    }

    public boolean isValidClassName(String className) {
        return className.matches("[A-Z][a-zA-Z0-9]*");
    }

}
