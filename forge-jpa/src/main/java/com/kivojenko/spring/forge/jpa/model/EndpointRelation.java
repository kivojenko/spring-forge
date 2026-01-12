package com.kivojenko.spring.forge.jpa.model;

import com.squareup.javapoet.TypeName;

public record EndpointRelation(
        String path,
        String getMethodName,
        TypeName elementType,
        boolean read,
        boolean add,
        boolean remove
) {}