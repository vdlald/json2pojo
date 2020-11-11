package com.vladislav.json2pojo;

import com.google.gson.*;
import com.squareup.javapoet.*;
import com.vladislav.json2pojo.pojo.ClassNameRef;
import com.vladislav.json2pojo.pojo.TypeSpecBuilderWrapper;
import lombok.RequiredArgsConstructor;

import javax.lang.model.element.Modifier;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.vladislav.json2pojo.Utils.*;

@SuppressWarnings("unused")
@RequiredArgsConstructor
public class GeneratePojoFromJson {

    private final int indent;
    private final FieldFactory fieldFactory;
    private final List<Consumer<TypeSpecBuilderWrapper>> afterCreationClass;
    private final List<Consumer<ClassNameRef>> beforeCreationClass;

    public static GeneratePojoFromJsonBuilder builder() {
        return new GeneratePojoFromJsonBuilder();
    }

    public JavaFile invoke(String packagePath, String className, String json) {
        final JsonObject jsonObject = JsonParser.parseString(json).getAsJsonObject();
        final TypeSpec.Builder typeSpecBuilder = jsonObjectToTypeSpec(packagePath, new ClassNameRef(className), jsonObject);
        final TypeSpec typeSpec = typeSpecBuilder.build();
        final JavaFile javaFile = JavaFile.builder(packagePath, typeSpec)
                .indent(" ".repeat(indent))
                .build();
        return javaFile;
    }

    private TypeSpec.Builder jsonObjectToTypeSpec(String packagePath, ClassNameRef className, JsonObject jsonObject) {
        TypeSpec.Builder classBuilder;
        if (!isValidClassName(className.className)) {
            className.className = toCamelCase(className.className, false);
        }
        final ClassNameRef classNameRef = new ClassNameRef(className.className);
        beforeCreationClass.forEach(consumer -> consumer.accept(classNameRef));
        className.className = classNameRef.className;

        classBuilder = TypeSpec.classBuilder(className.className);
        for (Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
            final String fieldName = entry.getKey();
            final JsonElement jsonElement = entry.getValue();
            FieldSpec fieldSpec;
            switch (determineTypeOfJsonElement(jsonElement)) {
                case PRIMITIVE:
                    final JsonPrimitive jsonPrimitive = jsonElement.getAsJsonPrimitive();
                    fieldSpec = jsonPrimitiveToFieldSpec(fieldName, jsonPrimitive);
                    break;
                case OBJECT:
                    final ClassNameRef nestedClassName = new ClassNameRef(capitalize(fieldName));
                    final JsonObject localJsonObject = jsonElement.getAsJsonObject();
                    final TypeSpec.Builder typeSpec = jsonObjectToTypeSpec(packagePath + "." + className.className,
                            nestedClassName, localJsonObject);
                    typeSpec.addModifiers(Modifier.STATIC);
                    classBuilder.addType(typeSpec.build());
                    final ClassName localClassName = ClassName.get(packagePath + "." + className.className,
                            nestedClassName.className);
                    fieldSpec = fieldFactory.createField(localClassName, fieldName);
                    break;
                case ARRAY:
                    final JsonArray jsonArray = jsonElement.getAsJsonArray();
                    fieldSpec = jsonArrayToFieldSpec(fieldName, jsonArray, classBuilder, packagePath, className);
                    break;
                default:
                    fieldSpec = fieldFactory.createObjectField(fieldName);
            }
            classBuilder.addField(fieldSpec);
        }
        afterCreationClass.forEach(consumer -> consumer.accept(new TypeSpecBuilderWrapper(fieldFactory, classBuilder, classNameRef.className)));
        return classBuilder;
    }

    private FieldSpec jsonArrayToFieldSpec(String fieldName, JsonArray jsonArray, TypeSpec.Builder classBuilder,
                                           String packagePath, ClassNameRef className) {
        FieldSpec fieldSpec;
        boolean isSameType = true;
        Set<JsonElementType> jsonElementsType = new HashSet<>();
        List<JsonElement> jsonElements = new ArrayList<>();
        for (JsonElement element : jsonArray) {
            final JsonElementType type = determineTypeOfJsonElement(element);
            if (!jsonElements.isEmpty()) {
                if (type == JsonElementType.NULL) {
                    continue;
                } else if (!jsonElementsType.contains(type)) {
                    isSameType = false;
                    break;
                }
            }
            jsonElements.add(element);
            jsonElementsType.add(type);
        }
        if (isSameType) {
            final JsonElementType type;
            final Iterator<JsonElementType> iterator = jsonElementsType.iterator();
            if (iterator.hasNext()) {
                type = iterator.next();
            } else {
                type = JsonElementType.OBJECT;
            }
            switch (type) {
                case PRIMITIVE:
                    final List<JsonPrimitive> jsonPrimitives = jsonElements.stream()
                            .map(JsonElement::getAsJsonPrimitive)
                            .collect(Collectors.toList());
                    final Set<JsonPrimitiveType> jsonPrimitiveTypes = jsonPrimitives.stream()
                            .map(Utils::determineTypeOfJsonPrimitive).collect(Collectors.toSet());
                    if (jsonPrimitiveTypes.size() == 1) {
                        final JsonPrimitiveType primitiveType = jsonPrimitiveTypes.iterator().next();
                        switch (primitiveType) {
                            case BOOLEAN:
                                fieldSpec = fieldFactory.createField(
                                        ParameterizedTypeName.get(List.class, Boolean.class), fieldName);
                                break;
                            case NUMBER:
                                final List<Number> numbers = jsonPrimitives.stream()
                                        .map(JsonPrimitive::getAsNumber).collect(Collectors.toList());
                                final Set<NumberType> numberTypes = numbers.stream()
                                        .map(Utils::determineTypeOfNumber).collect(Collectors.toSet());
                                if (numberTypes.size() == 1) {
                                    final NumberType numberType = numberTypes.iterator().next();
                                    switch (numberType) {
                                        case INT:
                                            fieldSpec = fieldFactory.createField(
                                                    ParameterizedTypeName.get(List.class, Integer.class),
                                                    fieldName);
                                            break;
                                        case LONG:
                                            fieldSpec = fieldFactory.createField(
                                                    ParameterizedTypeName.get(List.class, Long.class),
                                                    fieldName);
                                            break;
                                        default:
                                            fieldSpec = fieldFactory.createField(
                                                    ParameterizedTypeName.get(List.class, Double.class),
                                                    fieldName);
                                    }
                                } else {
                                    if (numberTypes.contains(NumberType.DOUBLE)) {
                                        fieldSpec = fieldFactory.createField(
                                                ParameterizedTypeName.get(List.class, Double.class),
                                                fieldName);
                                    } else {
                                        fieldSpec = fieldFactory.createField(
                                                ParameterizedTypeName.get(List.class, Long.class),
                                                fieldName);
                                    }
                                }
                                break;
                            default:
                                fieldSpec = fieldFactory.createField(
                                        ParameterizedTypeName.get(List.class, String.class),
                                        fieldName);
                                break;
                        }
                    } else {
                        fieldSpec = fieldFactory.createField(
                                ParameterizedTypeName.get(List.class, Object.class),
                                fieldName);
                    }
                    break;
                case OBJECT:
                    final List<JsonObject> jsonObjects = jsonElements.stream()
                            .map(JsonElement::getAsJsonObject)
                            .collect(Collectors.toList());
                    final List<Set<Map.Entry<String, JsonElement>>> entryStream = jsonObjects.stream()
                            .map(JsonObject::entrySet).collect(Collectors.toList());
                    if (entryStream.stream().map(Set::size).collect(Collectors.toSet()).size() == 1) {
                        final Set<String> fields = entryStream.stream().flatMap(Collection::stream)
                                .map(Map.Entry::getKey)
                                .collect(Collectors.toSet());
                        final JsonObject next = jsonObjects.iterator().next();
                        if (fields.size() == next.entrySet().size()) {  // we only made sure that all objects have the same fields, but we didn't make sure they had the same type.
                            final ClassNameRef nestedClassName2 = new ClassNameRef(capitalize(fieldName));
                            final TypeSpec.Builder typeSpec2 = jsonObjectToTypeSpec(packagePath + "." + className.className,
                                    nestedClassName2, next);
                            typeSpec2.addModifiers(Modifier.STATIC);
                            classBuilder.addType(typeSpec2.build());
                            final ClassName localClassName2 = ClassName.get(
                                    packagePath + "." + className.className, nestedClassName2.className);
                            fieldSpec = fieldFactory.createField(
                                    ParameterizedTypeName.get(ClassName.get(List.class), localClassName2), fieldName);
                        } else {
                            fieldSpec = fieldFactory.createField(
                                    ParameterizedTypeName.get(List.class, Object.class),
                                    fieldName);
                        }
                    } else {
                        fieldSpec = fieldFactory.createField(
                                ParameterizedTypeName.get(List.class, Object.class),
                                fieldName);
                    }
                    break;
                default:
                    // todo: implement JsonArray case
                    final List<JsonArray> jsonArrays = jsonElements.stream()
                            .map(JsonElement::getAsJsonArray)
                            .collect(Collectors.toList());

                    fieldSpec = fieldFactory.createField(
                            ParameterizedTypeName.get(ClassName.get(List.class),
                                    ParameterizedTypeName.get(List.class, Object.class)), fieldName);
                    break;
            }
        } else {
            fieldSpec = fieldFactory.createObjectField(fieldName);
        }
        return fieldSpec;
    }

    private FieldSpec jsonPrimitiveToFieldSpec(String fieldName, JsonPrimitive jsonPrimitive) {
        FieldSpec fieldSpec;
        switch (determineTypeOfJsonPrimitive(jsonPrimitive)) {
            case NUMBER:
                final Number number = jsonPrimitive.getAsNumber();
                switch (determineTypeOfNumber(number)) {
                    case INT:
                        fieldSpec = fieldFactory.createDoubleField(fieldName);
                        break;
                    case LONG:
                        fieldSpec = fieldFactory.createLongField(fieldName);
                        break;
                    default:
                        fieldSpec = fieldFactory.createIntField(fieldName);
                }
                break;
            case BOOLEAN:
                fieldSpec = fieldFactory.createBooleanField(fieldName);
                break;
            default:
                fieldSpec = fieldFactory.createField(TypeName.get(String.class), fieldName);
                break;
        }
        return fieldSpec;
    }

}
