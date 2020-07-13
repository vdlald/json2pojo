package com.vladislav.json2pojo;

import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.TypeName;
import com.vladislav.json2pojo.pojo.FieldNameRef;
import com.vladislav.json2pojo.pojo.FieldSpecBuilderWrapper;

import java.util.List;
import java.util.function.Consumer;

public class FieldFactory {
    private final List<Consumer<FieldSpecBuilderWrapper>> afterFieldCreationConsumers;
    private final List<Consumer<FieldNameRef>> beforeFieldCreationConsumers;
    private final TypeName doubleType;
    private final TypeName longType;
    private final TypeName intType;
    private final TypeName booleanType;
    private final TypeName objectType;

    FieldFactory(
            List<Consumer<FieldSpecBuilderWrapper>> afterFieldCreationConsumers,
            List<Consumer<FieldNameRef>> beforeFieldCreationConsumers, TypeName doubleType,
            TypeName longType,
            TypeName intType,
            TypeName booleanType
    ) {
        this.afterFieldCreationConsumers = afterFieldCreationConsumers;
        this.beforeFieldCreationConsumers = beforeFieldCreationConsumers;
        this.doubleType = doubleType;
        this.longType = longType;
        this.intType = intType;
        this.booleanType = booleanType;
        objectType = TypeName.get(Object.class);
    }

    public FieldSpec createField(TypeName typeName, String fieldName) {
        String fieldNameFormatted;
        if (Utils.isValidFieldName(fieldName)) {
            fieldNameFormatted = fieldName;
        } else {
            fieldNameFormatted = Utils.toCamelCase(fieldName, true);
        }
        final FieldNameRef fieldNameRef = new FieldNameRef(fieldNameFormatted);
        beforeFieldCreationConsumers.forEach(consumer -> consumer.accept(fieldNameRef));
        fieldNameFormatted = fieldNameRef.fieldName;

        final FieldSpec.Builder builder = FieldSpec.builder(typeName, fieldNameFormatted);
        afterFieldCreation(new FieldSpecBuilderWrapper(typeName, fieldName, fieldNameFormatted, builder));
        return builder.build();
    }

    public FieldSpec createDoubleField(String fieldName) {
        return createField(doubleType, fieldName);
    }

    public FieldSpec createIntField(String fieldName) {
        return createField(intType, fieldName);
    }

    public FieldSpec createLongField(String fieldName) {
        return createField(longType, fieldName);
    }

    public FieldSpec createBooleanField(String fieldName) {
        return createField(booleanType, fieldName);
    }

    public FieldSpec createObjectField(String fieldName) {
        return createField(objectType, fieldName);
    }

    private void afterFieldCreation(FieldSpecBuilderWrapper wrapper) {
        afterFieldCreationConsumers.forEach(builderConsumer -> builderConsumer.accept(wrapper));
    }
}
