package com.kivojenko.spring.forge.jpa.generator;


import com.kivojenko.spring.forge.jpa.factory.JpaEntityModelFactory;
import com.kivojenko.spring.forge.jpa.model.base.JpaEntityModel;
import com.squareup.javapoet.*;
import lombok.Setter;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import java.util.HashSet;
import java.util.Set;

/**
 * Generator for Spring Data JPA filter interfaces.
 */
public class FilterGenerator {
    @Setter
    private static ProcessingEnvironment env;

    private static final ClassName GETTER = ClassName.get("lombok", "Getter");
    private static final ClassName SETTER = ClassName.get("lombok", "Setter");
    private static final ClassName BUILDER = ClassName.get("lombok", "Builder");
    private static final ClassName DEFAULT = ClassName.get("lombok", "Builder", "Default");
    private static final ClassName ALL_ARGS = ClassName.get("lombok", "AllArgsConstructor");
    private static final ClassName REQUIRED_ARGS = ClassName.get("lombok", "RequiredArgsConstructor");
    private static final ClassName SET = ClassName.get(Set.class);
    private static final ClassName HASH_SET = ClassName.get(HashSet.class);


    /**
     * Generates a {@link JavaFile} containing the filter for the given model.
     *
     * @param model the entity model
     * @return the generated Java file
     */
    public static JavaFile generateFile(JpaEntityModel model) {
        return JavaFile.builder(model.packages().filterPackageName(), generate(model)).build();
    }

    /**
     * Generates the {@link TypeSpec} for the filter.
     *
     * @param model the entity model
     * @return the type specification
     */
    public static TypeSpec generate(JpaEntityModel model) {
        var elementUtils = env.getElementUtils();
        var typeUtils = env.getTypeUtils();
        var entityAnnotation = elementUtils.getTypeElement("jakarta.persistence.Entity");
        var iterableElement = elementUtils.getTypeElement("java.lang.Iterable");

        var builder = TypeSpec
                .classBuilder(model.filterName())
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(GETTER)
                .addAnnotation(SETTER)
                .addAnnotation(BUILDER)
                .addAnnotation(ALL_ARGS)
                .addAnnotation(REQUIRED_ARGS);

        for (var field : model.getFilterableFields()) {
            var fieldTypeMirror = field.asType();
            var name = field.getSimpleName().toString();
            TypeMirror entityCandidate = fieldTypeMirror;

            boolean isIterable = typeUtils.isAssignable(typeUtils.erasure(fieldTypeMirror), iterableElement.asType());
            if (isIterable) {
                if (fieldTypeMirror instanceof DeclaredType declared && !declared.getTypeArguments().isEmpty()) {
                    entityCandidate = declared.getTypeArguments().getFirst();
                }
            }

            var fieldTypeElement = (TypeElement) typeUtils.asElement(entityCandidate);
            TypeName fieldType = TypeName.get(fieldTypeMirror);
            FieldSpec.Builder fieldBuilder;

            if (fieldTypeElement != null &&
                    fieldTypeElement
                            .getAnnotationMirrors()
                            .stream()
                            .anyMatch(a -> typeUtils.isSameType(a.getAnnotationType(), entityAnnotation.asType()))) {
                var relation = JpaEntityModelFactory.get(fieldTypeElement);
                fieldType = ParameterizedTypeName.get(SET, relation.getFilterFieldType());
                if (!isIterable) name = relation.getFilterFieldName();
                fieldBuilder = FieldSpec
                        .builder(fieldType, name, Modifier.PRIVATE)
                        .addAnnotation(DEFAULT)
                        .initializer("new $T<>()", HASH_SET);
            } else {
                fieldBuilder = FieldSpec.builder(fieldType, name, Modifier.PRIVATE);
            }

            builder.addField(fieldBuilder.build());

        }

        return builder.build();
    }

}
