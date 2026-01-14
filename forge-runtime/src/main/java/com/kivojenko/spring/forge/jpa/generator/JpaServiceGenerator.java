package com.kivojenko.spring.forge.jpa.generator;

import com.kivojenko.spring.forge.jpa.model.model.JpaEntityModel;
import com.kivojenko.spring.forge.jpa.model.relation.EndpointRelation;
import com.kivojenko.spring.forge.jpa.service.ForgeService;
import com.kivojenko.spring.forge.jpa.service.HasNameForgeService;
import com.kivojenko.spring.forge.jpa.service.HasNameForgeServiceWithGetOrCreate;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;

import javax.lang.model.element.Modifier;

import static com.kivojenko.spring.forge.jpa.generator.MethodGenerator.getSetIdMethod;

/**
 * Generator for Spring services.
 */
public final class JpaServiceGenerator {

    private static final ClassName SERVICE = ClassName.get("org.springframework.stereotype", "Service");

    private static final ClassName FORGE_SERVICE = ClassName.get(ForgeService.class);
    private static final ClassName HAS_NAME_FORGE_SERVICE = ClassName.get(HasNameForgeService.class);
    private static final ClassName HAS_NAME_FORGE_SERVICE_WITH_GET_OR_CREATE =
            ClassName.get(HasNameForgeServiceWithGetOrCreate.class);

    /**
     * Generates a {@link JavaFile} containing the service for the given model.
     *
     * @param model the entity model
     * @return the generated Java file
     */
    public static JavaFile generateFile(JpaEntityModel model) {
        return JavaFile.builder(model.packages().servicePackageName(), generate(model)).build();
    }

    /**
     * Generates the {@link TypeSpec} for the service.
     *
     * @param model the entity model
     * @return the type specification
     */
    public static TypeSpec generate(JpaEntityModel model) {
        var spec = TypeSpec.classBuilder(model.serviceName()).addModifiers(Modifier.PUBLIC).addAnnotation(SERVICE);

        var superClassName = FORGE_SERVICE;
        if (model.requirements().hasName()) {
            superClassName = HAS_NAME_FORGE_SERVICE;

            if (model.requirements().wantsGetOrCreate()) {
                superClassName = HAS_NAME_FORGE_SERVICE_WITH_GET_OR_CREATE;
                spec.addMethod(model.resolveCreateMethod());
            }
        }

        var superClass = ParameterizedTypeName.get(superClassName,
                model.entityType(),
                model.jpaId().type(),
                model.repositoryType());

        model.endpointRelations().forEach(r -> addRelationEndpoints(spec, r));

        return spec.superclass(superClass).addMethod(getSetIdMethod(model)).build();
    }

    private static void addRelationEndpoints(TypeSpec.Builder spec, EndpointRelation relation) {
        var serviceMethod = relation.getServiceMethod();
        if (serviceMethod != null) spec.addMethod(serviceMethod);
        var field = relation.getServiceField();
        if (field != null) spec.addField(field);
    }

}
