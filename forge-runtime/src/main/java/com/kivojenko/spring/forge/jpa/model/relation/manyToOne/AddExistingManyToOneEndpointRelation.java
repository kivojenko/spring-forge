package com.kivojenko.spring.forge.jpa.model.relation.manyToOne;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import lombok.experimental.SuperBuilder;

import javax.lang.model.element.Modifier;

import static com.kivojenko.spring.forge.jpa.generator.MethodGenerator.*;

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
    protected MethodSpec.Builder getRepositoryMethodSpec() {
        var builder = MethodSpec
                .methodBuilder(generatedMethodName())
                .addModifiers(Modifier.PUBLIC)
                .returns(targetEntityModel.entityType())
                .addParameter(baseParamSpec());

        addFindBase(builder);
        addFindSub(builder);

        return builder
                .addStatement("$L.$L($L)", BASE_VAR_NAME, setterName(fieldName), SUB_VAR_NAME)
                .addStatement("return repository.save($L).$L()", BASE_VAR_NAME, getterName(fieldName));
    }
}

