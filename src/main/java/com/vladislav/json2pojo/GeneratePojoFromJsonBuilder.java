package com.vladislav.json2pojo;

import com.squareup.javapoet.TypeName;
import com.vladislav.json2pojo.pojo.ClassNameRef;
import com.vladislav.json2pojo.pojo.FieldNameRef;
import com.vladislav.json2pojo.pojo.FieldSpecBuilderWrapper;
import com.vladislav.json2pojo.pojo.TypeSpecBuilderWrapper;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@SuppressWarnings("unused")
public class GeneratePojoFromJsonBuilder {

    private int indent;
    private boolean usePrimitiveDataTypesInsteadBoxed;
    private boolean useDoubleNumbers;
    private boolean useLongIntegers;
    private final List<Consumer<FieldSpecBuilderWrapper>> afterFieldCreationConsumers;
    private final List<Consumer<TypeSpecBuilderWrapper>> afterCreationClass;
    private final List<Consumer<FieldNameRef>> beforeFieldCreationConsumers;
    private final List<Consumer<ClassNameRef>> beforeCreationClass;

    GeneratePojoFromJsonBuilder() {
        indent = 4;
        afterFieldCreationConsumers = new ArrayList<>();
        afterCreationClass = new ArrayList<>();
        beforeFieldCreationConsumers = new ArrayList<>();
        beforeCreationClass = new ArrayList<>();
        useBoxedPrimitive();
    }

    public GeneratePojoFromJsonBuilder useUnboxedPrimitive() {
        usePrimitiveDataTypesInsteadBoxed = true;
        return this;
    }

    public GeneratePojoFromJsonBuilder useBoxedPrimitive() {
        usePrimitiveDataTypesInsteadBoxed = false;
        return this;
    }

    public GeneratePojoFromJsonBuilder useDoubleNumbers(boolean state) {
        useDoubleNumbers = state;
        return this;
    }

    public GeneratePojoFromJsonBuilder useLongIntegers(boolean state) {
        useLongIntegers = state;
        return this;
    }

    public GeneratePojoFromJsonBuilder addAfterFieldCreationConsumers(Consumer<FieldSpecBuilderWrapper> consumer) {
        afterFieldCreationConsumers.add(consumer);
        return this;
    }

    public GeneratePojoFromJsonBuilder addAfterCreationClass(Consumer<TypeSpecBuilderWrapper> consumer) {
        afterCreationClass.add(consumer);
        return this;
    }

    public GeneratePojoFromJsonBuilder addBeforeFieldCreationConsumers(Consumer<FieldNameRef> consumer) {
        beforeFieldCreationConsumers.add(consumer);
        return this;
    }

    public GeneratePojoFromJsonBuilder addBeforeCreationClass(Consumer<ClassNameRef> consumer) {
        beforeCreationClass.add(consumer);
        return this;
    }

    public GeneratePojoFromJsonBuilder setIndent(int indent) {
        this.indent = indent;
        return this;
    }

    public GeneratePojoFromJson build() {
        TypeName longType;
        TypeName intType;
        TypeName doubleType = TypeName.DOUBLE;
        if (useDoubleNumbers) {
            intType = doubleType;
            longType = doubleType;
        } else if (useLongIntegers) {
            longType = TypeName.LONG;
            intType = longType;
        } else {
            longType = TypeName.LONG;
            intType = TypeName.INT;
        }
        TypeName booleanType = TypeName.BOOLEAN;
        if (!usePrimitiveDataTypesInsteadBoxed) {
            doubleType = doubleType.box();
            longType = longType.box();
            intType = intType.box();
            booleanType = booleanType.box();
        }
        final FieldFactory fieldFactory = new FieldFactory(afterFieldCreationConsumers, beforeFieldCreationConsumers,
                doubleType, longType, intType, booleanType);
        return new GeneratePojoFromJson(indent, fieldFactory, afterCreationClass, beforeCreationClass);
    }

}
