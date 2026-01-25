package com.kivojenko.spring.forge.jpa.factory;

import com.kivojenko.spring.forge.annotation.FilterField;
import com.kivojenko.spring.forge.jpa.model.FilterFieldModel;
import com.squareup.javapoet.TypeName;
import jakarta.persistence.Transient;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.util.ElementFilter;
import java.util.ArrayList;
import java.util.List;

public class FilterFieldModelFactory {
    public static List<FilterFieldModel> resolve(TypeElement entity, ProcessingEnvironment env) {
        var filterFields = new ArrayList<FilterFieldModel>();
        if (entity == null) return filterFields;

        var fields = ElementFilter.fieldsIn(entity.getEnclosedElements());
        var typeUtils = env.getTypeUtils();
        var elementUtils = env.getElementUtils();
        var iterableElement = elementUtils.getTypeElement("java.lang.Iterable");
        var entityAnnotation = elementUtils.getTypeElement("jakarta.persistence.Entity");

        for (var field : fields) {
            var hasFilter = field.getAnnotation(FilterField.class) != null;
            if (!hasFilter) continue;

            var isJavaTransient = field.getModifiers().contains(Modifier.TRANSIENT);
            var isJpaTransient = field.getAnnotation(Transient.class) != null;
            var isBeansTransient = field.getAnnotation(java.beans.Transient.class) != null;
            if (isJavaTransient || isJpaTransient || isBeansTransient) {
                throw new IllegalStateException("@FilterField is not allowed on transient field: " +
                        field.getSimpleName() +
                        " in " +
                        entity.getQualifiedName());
            }

            var type = field.asType();
            boolean isIterable = typeUtils.isAssignable(typeUtils.erasure(field.asType()), iterableElement.asType());

            var entityCandidate = isIterable &&
                    type instanceof DeclaredType declared &&
                    !declared.getTypeArguments().isEmpty() ? declared.getTypeArguments().getFirst() : type;

            var typeElement = typeUtils.asElement(entityCandidate);
            var singleEntity = !isIterable &&
                    typeElement != null &&
                    typeElement
                            .getAnnotationMirrors()
                            .stream()
                            .anyMatch(a -> typeUtils.isSameType(a.getAnnotationType(), entityAnnotation.asType()));

            filterFields.add(FilterFieldModel
                    .builder()
                    .element(field)
                    .type(type)
                    .typeElement((TypeElement) typeElement)
                    .typeName(TypeName.get(type))
                    .annotation(field.getAnnotation(FilterField.class))
                    .iterable(isIterable)
                    .singleEntity(singleEntity)
                    .entityCandidate(entityCandidate)
                    .env(env)
                    .build());
        }

        return filterFields;
    }
}
