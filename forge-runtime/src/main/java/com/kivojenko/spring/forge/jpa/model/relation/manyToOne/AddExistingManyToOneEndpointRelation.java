package com.kivojenko.spring.forge.jpa.model.relation.manyToOne;

import com.kivojenko.spring.forge.jpa.utils.StringUtils;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import lombok.experimental.SuperBuilder;

import javax.lang.model.element.Modifier;

import static com.kivojenko.spring.forge.jpa.utils.ClassNameUtils.PUT_MAPPING;
import static com.kivojenko.spring.forge.jpa.utils.StringUtils.capitalize;

/**
 * Represents a relation that generates a PUT endpoint to link an existing entity to a Many-to-One association.
 */
@SuperBuilder
public class AddExistingManyToOneEndpointRelation extends ManyToOneEndpointRelation {

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
                .returns(targetEntityModel.getEntityType())
                .addParameter(baseParamSpec());

        addFindBase(builder);
        addFindSub(builder);

        return builder
                .addStatement("$L.$L($L)", BASE_VAR_NAME, StringUtils.setterName(fieldName), SUB_VAR_NAME)
                .addStatement("return repository.save($L).$L()", BASE_VAR_NAME, StringUtils.getterName(fieldName))
                .build();
    }
}

