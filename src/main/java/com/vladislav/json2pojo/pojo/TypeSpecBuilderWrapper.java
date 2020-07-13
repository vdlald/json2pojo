package com.vladislav.json2pojo.pojo;

import com.squareup.javapoet.TypeSpec;
import com.vladislav.json2pojo.FieldFactory;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class TypeSpecBuilderWrapper {
    public final FieldFactory fieldFactory;
    public final TypeSpec.Builder typeSpec;
    public final String className;
}
