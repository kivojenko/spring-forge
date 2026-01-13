package com.kivojenko.spring.forge.jpa.model.factory;

import com.kivojenko.spring.forge.jpa.model.model.JpaEntityModel;
import com.squareup.javapoet.ClassName;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.kivojenko.spring.forge.jpa.model.model.JpaEntityModelRequirements.resolveRequirements;
import static com.kivojenko.spring.forge.jpa.model.model.JpaEntityPackageNames.resolvePackageNames;
import static com.kivojenko.spring.forge.jpa.model.model.JpaId.resolveId;


public final class JpaEntityModelFactory {

    private static final Map<TypeElement, JpaEntityModel> CACHE = new HashMap<>();

    public static List<JpaEntityModel> getAll() {
        return CACHE.values().stream().toList();
    }

    public static JpaEntityModel get(TypeElement entity, ProcessingEnvironment env) {
        var cached = CACHE.get(entity);
        if (cached != null) return cached;

        var entityType = ClassName.get(entity);

        var entityId = resolveId(entity);
        var packages = resolvePackageNames(entity, env);
        var requirements = resolveRequirements(entity, env);
        var model = new JpaEntityModel(entity, entityType, entityId, packages, requirements, env);

        CACHE.put(entity, model);
        return model;
    }


}
