package com.kivojenko.spring.forge.jpa.model.relation.manyToOne;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import lombok.experimental.SuperBuilder;

import javax.lang.model.element.Modifier;

import static com.kivojenko.spring.forge.jpa.utils.StringUtils.setterName;
import static com.kivojenko.spring.forge.jpa.utils.ClassNameUtils.DELETE_MAPPING;
import static com.kivojenko.spring.forge.jpa.utils.StringUtils.capitalize;

/**
 * Represents a relation that generates a DELETE endpoint to remove (unlink) an entity from a Many-to-One association.
 */
@SuperBuilder
public class RemoveManyToOneEndpointRelation extends ManyToOneEndpointRelation {

    @Override
    protected ClassName mapping() {
        return DELETE_MAPPING;
    }

    protected String uri() {
        return super.uri() + "/{" + SUB_ID_PARAM_NAME + "}";
    }

    @Override
    protected String generatedMethodName() {
        return "remove" + capitalize(fieldName);
    }

    @Override
    public MethodSpec getServiceMethod() {
        var builder = MethodSpec
                .methodBuilder((generatedMethodName()))
                .returns(void.class)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(Long.class, BASE_ID_PARAM_NAME);

        addFindBase(builder);
        addFindSub(builder);
        return builder.addStatement("$L.$L(null)", BASE_VAR_NAME, setterName(fieldName)).build();
    }

}
