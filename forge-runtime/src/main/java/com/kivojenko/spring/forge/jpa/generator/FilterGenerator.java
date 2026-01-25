package com.kivojenko.spring.forge.jpa.generator;


import com.kivojenko.spring.forge.jpa.contract.HasToPredicate;
import com.kivojenko.spring.forge.jpa.model.base.JpaEntityModel;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;

import javax.lang.model.element.Modifier;

import static com.kivojenko.spring.forge.jpa.utils.ClassNameUtils.*;

/**
 * Generator for Spring Data JPA filter interfaces.
 */
public class FilterGenerator {
    public static final String BUILDER_VAR_NAME = "builder";
    public static final String ENTITY_VAR_NAME = "entity";

    /**
     * Generates a {@link JavaFile} containing the filter for the given model.
     *
     * @param model the entity model
     * @return the generated Java file
     */
    public static JavaFile generateFile(JpaEntityModel model) {
        return JavaFile.builder(model.getPackages().filterPackageName(), generate(model)).build();
    }

    /**
     * Generates the {@link TypeSpec} for the filter.
     *
     * @param model the entity model
     * @return the type specification
     */
    public static TypeSpec generate(JpaEntityModel model) {
        var builder = TypeSpec
                .classBuilder(model.getFilterName())
                .addSuperinterface(HasToPredicate.class)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(GETTER)
                .addAnnotation(SETTER)
                .addAnnotation(TO_STRING)
                .addAnnotation(BUILDER)
                .addAnnotation(ALL_ARGS)
                .addAnnotation(REQUIRED_ARGS);

        for (var field : model.getFilterableFields()) {
            builder.addField(field.getFieldSpec());
        }

        builder.addMethod(model.toPredicateMethod());

        return builder.build();
    }

}
