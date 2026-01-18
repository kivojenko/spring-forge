package com.kivojenko.spring.forge.jpa.model.relation.oneToMany;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import lombok.experimental.SuperBuilder;

import javax.lang.model.element.Modifier;

import static com.kivojenko.spring.forge.jpa.generator.MethodGenerator.DELETE_MAPPING;
import static com.kivojenko.spring.forge.jpa.generator.MethodGenerator.setterName;
import static com.kivojenko.spring.forge.jpa.utils.StringUtils.capitalize;

/**
 * Represents a relation that generates a DELETE endpoint to remove an entity from a OneToMany association.
 */
@SuperBuilder
public class RemoveOneToManyEndpointRelation extends OneToManyEndpointRelation {
    @Override
    protected ClassName mapping() {
        return DELETE_MAPPING;
    }

    @Override
    protected String uri() {
        return super.uri() + "/{" + SUB_ID_PARAM_NAME + "}";
    }

    @Override
    protected String generatedMethodName() {
        return "remove" + capitalize(fieldName);
    }

    @Override
    protected MethodSpec.Builder getRepositoryMethodSpec() {
        var builder = MethodSpec
                .methodBuilder((generatedMethodName()))
                .returns(void.class)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(baseParamSpec());
        addFindSub(builder);
        return builder.addStatement("$L.$L(null)", SUB_VAR_NAME, setterName(mappedBy));
    }

}
