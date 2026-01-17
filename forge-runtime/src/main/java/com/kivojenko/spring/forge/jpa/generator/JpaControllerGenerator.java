package com.kivojenko.spring.forge.jpa.generator;

import com.kivojenko.spring.forge.jpa.controller.ForgeController;
import com.kivojenko.spring.forge.jpa.controller.HasNameForgeController;
import com.kivojenko.spring.forge.jpa.controller.HasNameForgeControllerWithService;
import com.kivojenko.spring.forge.jpa.controller.ForgeControllerWithService;
import com.kivojenko.spring.forge.jpa.model.relation.EndpointRelation;
import com.kivojenko.spring.forge.jpa.model.model.JpaEntityModel;
import com.squareup.javapoet.*;

import javax.lang.model.element.Modifier;

import static com.kivojenko.spring.forge.jpa.generator.MethodGenerator.getSetIdMethod;

/**
 * Generator for Spring REST controllers.
 */
public final class JpaControllerGenerator {

    private static final ClassName REST_CONTROLLER =
            ClassName.get("org.springframework.web.bind.annotation", "RestController");
    private static final ClassName REQUEST_MAPPING =
            ClassName.get("org.springframework.web.bind.annotation", "RequestMapping");

    private static final ClassName ABSTRACT_CONTROLLER = ClassName.get(ForgeController.class);
    private static final ClassName HAS_NAME_CONTROLLER = ClassName.get(HasNameForgeController.class);
    private static final ClassName HAS_SERVICE_CONTROLLER = ClassName.get(ForgeControllerWithService.class);
    private static final ClassName HAS_NAME_WITH_SERVICE_CONTROLLER =
            ClassName.get(HasNameForgeControllerWithService.class);

    /**
     * Generates a {@link JavaFile} containing the REST controller for the given model.
     *
     * @param model the entity model
     * @return the generated Java file
     */
    public static JavaFile generateFile(JpaEntityModel model) {
        return JavaFile.builder(model.packages().controllerPackageName(), generate(model)).build();
    }

    /**
     * Generates the {@link TypeSpec} for the REST controller.
     *
     * @param model the entity model
     * @return the type specification
     */
    public static TypeSpec generate(JpaEntityModel model) {
        var spec = TypeSpec.classBuilder(model.controllerName())
                .addModifiers(Modifier.PUBLIC)
                .superclass(getSuperClass(model));

        if (model.requirements().wantsAbstractController()) {
            spec.addModifiers(Modifier.ABSTRACT);
        } else if (model.requirements().wantsImplementedController()) {
            var mappingAnnotation = AnnotationSpec
                            .builder(REQUEST_MAPPING)
                            .addMember("value", "$S", model.controllerPath())
                            .build();
            spec.addAnnotation(REST_CONTROLLER).addAnnotation(mappingAnnotation);
        }

        if (!model.requirements().wantsService()) {
            spec.addMethod(getSetIdMethod(model));
        }

        model.endpointRelations().forEach(r -> addRelationEndpoints(spec, r));

        return spec.build();
    }

    /**
     * Determines the superclass for the generated controller.
     *
     * @param model the entity model
     * @return the parameterized type name of the superclass
     */
    public static ParameterizedTypeName getSuperClass(JpaEntityModel model) {
        return model.requirements().wantsService() ? withService(model) : withoutService(model);
    }

    private static ParameterizedTypeName withService(JpaEntityModel model) {
        var superClass = model.requirements().hasName() ? HAS_NAME_WITH_SERVICE_CONTROLLER : HAS_SERVICE_CONTROLLER;

        return ParameterizedTypeName.get(superClass,
                model.entityType(),
                model.jpaId().type(),
                model.repositoryType(),
                model.serviceType());
    }

    private static ParameterizedTypeName withoutService(JpaEntityModel model) {
        var superClass = model.requirements().hasName() ? HAS_NAME_CONTROLLER : ABSTRACT_CONTROLLER;

        return ParameterizedTypeName.get(superClass, model.entityType(), model.jpaId().type(), model.repositoryType());
    }

    private static void addRelationEndpoints(TypeSpec.Builder spec, EndpointRelation relation) {
        var method = relation.getControllerMethod();
        if (method != null) spec.addMethod(method);
        var field = relation.getControllerField();
        if (field != null) spec.addField(field);
    }
}
