package com.vladislav.json2pojo.pojo;

import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.TypeName;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class FieldSpecBuilderWrapper {
    public final TypeName typeName;
    public final String fieldName;
    public final String fieldNameFormatted;
    public final FieldSpec.Builder fieldBuilder;
}
