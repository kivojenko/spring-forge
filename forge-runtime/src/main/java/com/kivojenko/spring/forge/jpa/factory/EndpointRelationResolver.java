package com.kivojenko.spring.forge.jpa.factory;

import com.kivojenko.spring.forge.annotation.endpoint.WithEndpoints;
import com.kivojenko.spring.forge.annotation.endpoint.WithGetEndpoint;
import com.kivojenko.spring.forge.jpa.model.base.JpaEntityModel;
import com.kivojenko.spring.forge.jpa.model.relation.EndpointRelation;
import com.kivojenko.spring.forge.jpa.model.relation.EndpointRelationFactory;
import com.kivojenko.spring.forge.jpa.model.relation.toCollection.manyToMany.ManyToManyEndpointRelationFactory;
import com.kivojenko.spring.forge.jpa.model.relation.toCollection.oneToMany.OneToManyEndpointRelationFactory;
import com.kivojenko.spring.forge.jpa.model.relation.toCollection.oneToMany.ReadOneToManyEndpointRelation;
import com.kivojenko.spring.forge.jpa.model.relation.toSingle.manyToOne.ManyToOneEndpointRelationFactory;
import com.kivojenko.spring.forge.jpa.model.relation.toSingle.oneToOne.OneToOneEndpointRelationFactory;
import com.kivojenko.spring.forge.jpa.utils.LoggingUtils;
import jakarta.persistence.*;
import org.jspecify.annotations.NonNull;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import java.util.ArrayList;
import java.util.List;

import static java.beans.Introspector.decapitalize;

/**
 * Factory for resolving {@link EndpointRelation}s from entity elements.
 */
public class EndpointRelationResolver {

  /**
   * Resolves all endpoint relations for the given entity.
   * It scans for fields annotated with {@link WithEndpoints} and methods annotated with {@link WithGetEndpoint}.
   *
   * @param entity the entity type element
   * @param env    the processing environment
   * @return a list of resolved endpoint relations
   */
  public static List<EndpointRelation> resolve(TypeElement entity, ProcessingEnvironment env) {
    var endpointRelations = new ArrayList<EndpointRelation>();

    for (var enclosed : entity.getEnclosedElements()) {
      if (enclosed instanceof VariableElement field) {
        endpointRelations.addAll(resolveFieldEndpointRelation(field, env));
      } else if (enclosed instanceof ExecutableElement getter) {
        var relation = resolveGetterEndpointRelation(getter, env);
        if (relation != null) endpointRelations.add(relation);
      }

    }
    endpointRelations.forEach(r -> r.setEntityModel(JpaEntityModelFactory.get(entity)));
    return endpointRelations;

  }

  private static @NonNull List<EndpointRelation> resolveFieldEndpointRelation(
      VariableElement field,
      ProcessingEnvironment env
  ) {
    var result = new ArrayList<EndpointRelation>();

    var withEndpoints = field.getAnnotation(WithEndpoints.class);
    if (withEndpoints == null) return result;

    var factory = resolveFactory(field, env);
    if (factory == null) return result;

    if (withEndpoints.read()) {
      var relation = factory.getReadRelation();
      if (relation != null) result.add(relation);
    }

    if (withEndpoints.addNew()) {
      var relation = factory.getAddNewRelation();
      if (relation != null) result.add(relation);
    }

    if (withEndpoints.linkExisting()) {
      var relation = factory.getLinkExistingRelation();
      if (relation != null) result.add(relation);
    }

    if (withEndpoints.remove()) {
      var relation = factory.getUnlinkRelation();
      if (relation != null) result.add(relation);
    }

    return result;
  }

  private static EndpointRelationFactory resolveFactory(VariableElement field, ProcessingEnvironment env) {
    var withEndpoints = field.getAnnotation(WithEndpoints.class);
    if (withEndpoints == null) throw new RuntimeException();

    var path = withEndpoints.path();
    if (path.isBlank()) path = field.getSimpleName().toString();

    var embedded = field.getAnnotation(Embedded.class);
    var oneToOne = field.getAnnotation(OneToOne.class);

    if (embedded != null || oneToOne != null) {
      return OneToOneEndpointRelationFactory
          .builder()
          .path(path)
          .field(field)
          .targetEntityModel(getEntityModel(field, env))
          .build();
    }

    var manyToOne = field.getAnnotation(ManyToOne.class);
    if (manyToOne != null) {
      return ManyToOneEndpointRelationFactory
          .builder()
          .path(path)
          .field(field)
          .targetEntityModel(getEntityModel(field, env))
          .build();
    }


    var oneToMany = field.getAnnotation(OneToMany.class);
    if (oneToMany != null) {
      var mappedBy = oneToMany.mappedBy();
      if (mappedBy.isBlank()) {
        LoggingUtils.warn(env, field, "@WithEndpoints can only be used on OneToMany associations with mappedBy");
      } else {
        return OneToManyEndpointRelationFactory
            .builder()
            .path(path)
            .mappedBy(mappedBy)
            .field(field)
            .targetEntityModel(getEntityModelFromList(field.asType(), field, env))
            .build();
      }
    }

    var manyToMany = field.getAnnotation(ManyToMany.class);
    if (manyToMany != null) {
      return ManyToManyEndpointRelationFactory
          .builder()
          .path(path)
          .field(field)
          .targetEntityModel(getEntityModelFromList(field.asType(), field, env))
          .build();
    }
    return null;
  }

  private static EndpointRelation resolveGetterEndpointRelation(ExecutableElement getter, ProcessingEnvironment env) {
    var withGetEndpoint = getter.getAnnotation(WithGetEndpoint.class);
    if (withGetEndpoint == null) return null;

    if (!getter.getModifiers().contains(Modifier.PUBLIC)) {
      LoggingUtils.warn(env, getter, "@WithGetEndpoints can only be used on public methods");
      return null;
    }

    var path = withGetEndpoint.path();

    if (path.isBlank()) {
      path = getter.getSimpleName().toString();
      if (getter.getSimpleName().toString().startsWith("get")) {
        path = decapitalize(path.substring(3));
      }
    }

    return ReadOneToManyEndpointRelation
        .builder()
        .path(path)
        .methodName(getter.getSimpleName().toString())
        .targetEntityModel(getEntityModelFromList(getter.getReturnType(), getter, env))
        .build();
  }

  private static JpaEntityModel getEntityModel(Element element, ProcessingEnvironment env) {
    return JpaEntityModelFactory.get((TypeElement) env.getTypeUtils().asElement(element.asType()));
  }

  private static JpaEntityModel getEntityModelFromList(
      TypeMirror returnType,
      Element element,
      ProcessingEnvironment env
  ) {

    if (!(returnType instanceof DeclaredType declaredReturnType)) {
      LoggingUtils.warn(env, element, "Generating endpoints can only be used on declared return types");
      return null;
    }

    var typeArgs = declaredReturnType.getTypeArguments();
    if (typeArgs.size() != 1) {
      LoggingUtils.warn(env, element, "Generating endpoints must return a generic collection");
      return null;
    }

    if (!(typeArgs.getFirst() instanceof DeclaredType elementDeclaredType)) {
      LoggingUtils.warn(env, element, "Generating endpoints collection element type must be a declared type");
      return null;
    }

    var asElement = elementDeclaredType.asElement();
    if (!(asElement instanceof TypeElement elementType)) {
      LoggingUtils.warn(env, element, "Generating endpoints collection element is not a TypeElement");
      return null;
    }

    return JpaEntityModelFactory.get(elementType);
  }

}
