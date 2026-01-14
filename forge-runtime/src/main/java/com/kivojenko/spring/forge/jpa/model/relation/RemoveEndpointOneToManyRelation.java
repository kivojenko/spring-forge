package com.kivojenko.spring.forge.jpa.model.relation;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import lombok.experimental.SuperBuilder;

import javax.lang.model.element.Modifier;

import static com.kivojenko.spring.forge.jpa.generator.MethodGenerator.DELETE_MAPPING;
import static com.kivojenko.spring.forge.jpa.generator.MethodGenerator.getterName;
import static com.kivojenko.spring.forge.jpa.utils.StringUtils.capitalize;
import static com.kivojenko.spring.forge.jpa.utils.StringUtils.decapitalize;

/**
 * Represents a relation that generates a DELETE endpoint to remove an entity from a OneToMany association.
 */
@SuperBuilder
public class RemoveEndpointOneToManyRelation extends EndpointRelation {
    public static final ClassName AUTOWIRED =
            ClassName.get("org.springframework.beans.factory.annotation", "Autowired");
    private String mappedBy;

    @Override
    public FieldSpec getControllerField() {
        if (entityModel.requirements().wantsService()) {
            return null;
        }
        return getRepositoryFieldSpec();
    }

    @Override
    public FieldSpec getServiceField() {
        if (entityModel.requirements().wantsService()) {
            return getRepositoryFieldSpec();
        }
        return null;
    }

    private FieldSpec getRepositoryFieldSpec() {
        return FieldSpec.builder(targetEntityModel.repositoryType(),
                decapitalize(targetEntityModel.repositoryName()),
                Modifier.PRIVATE).addAnnotation(AnnotationSpec.builder(AUTOWIRED).build()).build();
    }

    @Override
    public MethodSpec getControllerMethod() {
        var method = getRepositoryMethodSpec();
        if (entityModel.requirements().wantsService()) {
            method = getServiceMethodSpec();
        }
        return method.addAnnotation(AnnotationSpec.builder(DELETE_MAPPING)
                .addMember("value", "$S", "/{id}/" + path)
                .build()).build();
    }

    @Override
    public MethodSpec getServiceMethod() {
        if (entityModel.requirements().wantsService()) {
            return getRepositoryMethodSpec().build();
        }
        return null;
    }

    private MethodSpec.Builder getServiceMethodSpec() {
        var methodName = "remove" + capitalize(fieldName);
        var subIdName = targetEntityModel.element().getSimpleName() + "Id";
        var useService = "service." + methodName + "(id, " + subIdName + ")";

        return MethodSpec.methodBuilder(("remove" + capitalize(fieldName)))
                .addModifiers(Modifier.PUBLIC)
                .addParameter(Long.class, "id")
                .addParameter(Long.class, subIdName)
                .addException(ClassName.get("jakarta.persistence", "EntityNotFoundException"))
                .addStatement(useService);
    }

    private MethodSpec.Builder getRepositoryMethodSpec() {
        var subIdName = targetEntityModel.element().getSimpleName() + "Id";
        var findSub = "var sub = " +
                decapitalize(targetEntityModel.repositoryName()) +
                ".findById(" +
                subIdName +
                ").orElseThrow" +
                "(EntityNotFoundException::new)";
        var ifWrongSub = "if (!sub.get" + capitalize(mappedBy) + "()." + getterName(entityModel) + "().equals(id))";
        var setNullEntity = "sub.set" + capitalize(mappedBy) + "(null)";

        return MethodSpec.methodBuilder(("remove" + capitalize(fieldName)))
                .addModifiers(Modifier.PUBLIC)
                .addParameter(Long.class, "id")
                .addParameter(Long.class, subIdName)
                .addException(ClassName.get("jakarta.persistence", "EntityNotFoundException"))
                .addStatement(findSub)
                .beginControlFlow(ifWrongSub)
                .addStatement("throw new EntityNotFoundException()")
                .endControlFlow()
                .addStatement(setNullEntity);
    }

}
