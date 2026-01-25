package com.kivojenko.spring.forge.jpa.model.relation.manyToMany;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import lombok.experimental.SuperBuilder;

import javax.lang.model.element.Modifier;

import static com.kivojenko.spring.forge.jpa.utils.ClassNameUtils.ITERABLE;
import static com.kivojenko.spring.forge.jpa.utils.ClassNameUtils.PUT_MAPPING;
import static com.kivojenko.spring.forge.jpa.utils.StringUtils.capitalize;
import static com.kivojenko.spring.forge.jpa.utils.StringUtils.getterName;

/**
 * Represents a relation that generates a PUT endpoint to link an existing entity to a Many-to-One association.
 */
@SuperBuilder
public class AddManyToManyEndpointRelation extends ManyToManyEndpointRelation {

    @Override
    protected String generatedMethodName() {
        return "addExisting" + capitalize(fieldName);
    }

    @Override
    protected String uri() {
        return super.uri() + "/{" + SUB_ID_PARAM_NAME + "}";
    }

    @Override
    protected ClassName mapping() {
        return PUT_MAPPING;
    }

    @Override
    public MethodSpec getServiceMethod() {
        var builder = MethodSpec
                .methodBuilder(generatedMethodName())
                .addModifiers(Modifier.PUBLIC)
                .returns(ParameterizedTypeName.get(ITERABLE, targetEntityModel.getEntityType()))
                .addParameter(ParameterSpec.builder(entityModel.getJpaId().type(), BASE_ID_PARAM_NAME).build());

        addFindBase(builder);
        addFindSub(builder);

        return builder
                .beginControlFlow("if ($L.$L().contains($L))", BASE_VAR_NAME, getterName(fieldName), SUB_VAR_NAME)
                .addStatement("return $L.$L()", BASE_VAR_NAME, getterName(fieldName))
                .endControlFlow()
                .addStatement("$L.$L().add($L)", BASE_VAR_NAME, getterName(fieldName), SUB_VAR_NAME)
                .addStatement("return repository.save($L).$L()", BASE_VAR_NAME, getterName(fieldName))
                .build();
    }
}

