package com.kivojenko.spring.forge.jpa.model.relation.toCollection.manyToMany;

import com.kivojenko.spring.forge.jpa.model.relation.ServiceRepositoryEndpointRelation;
import com.kivojenko.spring.forge.jpa.utils.HttpStatusValue;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import lombok.experimental.SuperBuilder;

import javax.lang.model.element.Modifier;

import static com.kivojenko.spring.forge.jpa.utils.ClassNameUtils.DELETE_MAPPING;
import static com.kivojenko.spring.forge.jpa.utils.ClassNameUtils.TRANSACTIONAL;
import static com.kivojenko.spring.forge.jpa.utils.HttpStatusValue.NO_CONTENT;
import static com.kivojenko.spring.forge.jpa.utils.StringUtils.*;

/**
 * Represents a relation that generates a DELETE endpoint to remove (unlink) an entity from a Many-to-Many association.
 */
@SuperBuilder
public class UnlinkManyToManyEndpointRelation extends ServiceRepositoryEndpointRelation {

  @Override
  protected ClassName mapping() {
    return DELETE_MAPPING;
  }

  @Override
  protected HttpStatusValue httpStatus() {
    return NO_CONTENT;
  }

  protected String uri() {
    return super.uri() + "/{" + subIdParamName() + "}";
  }

  @Override
  protected String generatedMethodName() {
    return "removeRelationWith" + capitalize(singularize(getFieldName()));
  }

  @Override
  public MethodSpec getServiceMethod() {
    var builder = MethodSpec
        .methodBuilder(generatedMethodName())
        .addJavadoc("Removes (unlinks) an existing {@link $T} entity from a {@link $T} entity.\n", targetEntityModel.getEntityType(), entityModel.getEntityType())
        .addJavadoc("@param $L the ID of the {@link $T} entity\n", baseIdParamName(), entityModel.getEntityType())
        .addJavadoc("@param $L the ID of the {@link $T} entity to remove\n", subIdParamName(), targetEntityModel.getEntityType())
        .addModifiers(Modifier.PUBLIC)
        .addAnnotation(TRANSACTIONAL)
        .returns(void.class);

    addFindBase(builder);
    builder.addParameter(subParamSpec(false));

    return builder
        .addStatement(
            "$L.$L().removeIf(e -> $T.equals(e.$L(), $L))",
            BASE_VAR_NAME,
            getterName(getFieldName()),
            java.util.Objects.class,
            getterName(targetEntityModel.getJpaId().name()),
            subIdParamName()
        )
        .build();
  }

}
