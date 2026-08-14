package com.kivojenko.spring.forge.jpa.generator;

import static com.kivojenko.spring.forge.jpa.utils.ClassNameUtils.DELETE_MAPPING;
import static com.kivojenko.spring.forge.jpa.utils.ClassNameUtils.FORGE_ABSTRACT_CONTROLLER;
import static com.kivojenko.spring.forge.jpa.utils.ClassNameUtils.FORGE_CONTROLLER;
import static com.kivojenko.spring.forge.jpa.utils.ClassNameUtils.GET_MAPPING;
import static com.kivojenko.spring.forge.jpa.utils.ClassNameUtils.HTTP_STATUS;
import static com.kivojenko.spring.forge.jpa.utils.ClassNameUtils.PAGE;
import static com.kivojenko.spring.forge.jpa.utils.ClassNameUtils.PAGEABLE;
import static com.kivojenko.spring.forge.jpa.utils.ClassNameUtils.PAGEABLE_DEFAULT;
import static com.kivojenko.spring.forge.jpa.utils.ClassNameUtils.PATH_VARIABLE;
import static com.kivojenko.spring.forge.jpa.utils.ClassNameUtils.POST_MAPPING;
import static com.kivojenko.spring.forge.jpa.utils.ClassNameUtils.PUT_MAPPING;
import static com.kivojenko.spring.forge.jpa.utils.ClassNameUtils.REQUEST_BODY;
import static com.kivojenko.spring.forge.jpa.utils.ClassNameUtils.REQUEST_MAPPING;
import static com.kivojenko.spring.forge.jpa.utils.ClassNameUtils.REQUEST_METHOD;
import static com.kivojenko.spring.forge.jpa.utils.ClassNameUtils.REQUEST_PARAM;
import static com.kivojenko.spring.forge.jpa.utils.ClassNameUtils.RESPONSE_STATUS;
import static com.kivojenko.spring.forge.jpa.utils.ClassNameUtils.REST_CONTROLLER;
import static com.kivojenko.spring.forge.jpa.utils.ClassNameUtils.VALID;

import com.kivojenko.spring.forge.config.SpringForgeConfig;
import com.kivojenko.spring.forge.jpa.model.base.JpaEntityModel;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import javax.lang.model.element.Modifier;

/**
 * Generator for Spring REST controllers.
 */
public final class ControllerGenerator {

  /**
   * Generates a {@link JavaFile} containing the REST controller for the given model.
   *
   * @param model the entity model
   * @return the generated Java file
   */
  public static JavaFile generateFile(JpaEntityModel model) {
    return JavaFile.builder(model.getPackages().controllerPackageName(), generate(model)).build();
  }

  /**
   * Generates the {@link TypeSpec} for the REST controller.
   *
   * @param model the entity model
   * @return the type specification
   */
  public static TypeSpec generate(JpaEntityModel model) {
    var baseController = model.isAbstract() ? FORGE_ABSTRACT_CONTROLLER : FORGE_CONTROLLER;
    var superClass = ParameterizedTypeName.get(
        baseController,
        model.getEntityType(),
        model.getJpaId().type(),
        model.getRepositoryType(),
        model.getServiceType()
    );
    var builder = TypeSpec.classBuilder(model.getControllerName()).addModifiers(Modifier.PUBLIC).superclass(superClass);

    var javadoc = CodeBlock
        .builder()
        .add("Generated REST controller for {@link $T}.\n", model.getEntityType())
        .add("Provides endpoints for standard CRUD operations and custom relations.\n")
        .build();
    builder.addJavadoc(javadoc);

    if (model.getRequirements().wantsAbstractController()) {
      builder.addModifiers(Modifier.ABSTRACT);
    } else {
      var mappingAnnotation = AnnotationSpec
          .builder(REQUEST_MAPPING)
          .addMember("value", "$S", model.getControllerPath())
          .build();
      builder.addAnnotation(REST_CONTROLLER).addAnnotation(mappingAnnotation);
    }

    if (model.getRequirements().getOrCreateAnnotation() != null && !model.isAbstract()) {
      var cfg = model.getRequirements().getOrCreateAnnotation();
      var path = cfg.path().isEmpty() ? "/get-or-create" : cfg.path();
      var mapping = AnnotationSpec.builder(POST_MAPPING).addMember("value", "$S", path).build();
      var field = cfg.field().isEmpty() ? "name" : cfg.field();
      var fieldType = model.resolveFieldTypeName(field);

      var param = ParameterSpec.builder(fieldType, field).addAnnotation(REQUEST_PARAM).build();
      var getOrCreate = MethodSpec
          .methodBuilder("getOrCreate")
          .addJavadoc("Retrieves an existing {@link $T} by $L or creates it if it does not exist.\n", model.getEntityType(), field)
          .addJavadoc("@param $L the $L of the entity\n", field, field)
          .addJavadoc("@return the retrieved or newly created entity\n")
          .addModifiers(Modifier.PUBLIC)
          .addAnnotation(mapping)
          .addParameter(param)
          .returns(model.getEntityType())
          .addStatement("return service.getOrCreate($L)", field)
          .build();
      builder.addMethod(getOrCreate);
    }

    var pageableAnnotation = AnnotationSpec
        .builder(PAGEABLE_DEFAULT)
        .addMember("size", "$L", SpringForgeConfig.getAllPageSize)
        .build();
    var pageableParam = ParameterSpec.builder(PAGEABLE, "pageable").addAnnotation(pageableAnnotation).build();
    var findAllBuilder = MethodSpec
        .methodBuilder("findAll")
        .addJavadoc("Retrieves a paged result of all {@link $T} entities.\n", model.getEntityType())
        .addJavadoc("@param pageable the pagination information\n")
        .addModifiers(Modifier.PUBLIC)
        .addAnnotation(GET_MAPPING)
        .returns(ParameterizedTypeName.get(PAGE, model.getEntityType()))
        .addParameter(pageableParam);

    if (model.wantsFilter()) {
      var filterParam = ParameterSpec.builder(model.getFilterType(), "filter")
          .addAnnotation(VALID)
          .build();
      findAllBuilder
          .addParameter(filterParam)
          .addJavadoc("@param filter the filter criteria\n")
          .addJavadoc("@return a page of entities matching the filter criteria\n")
          .addStatement("return service.findAll(pageable, filter)");
      builder.addMethod(findAllBuilder.build());
    } else {
      findAllBuilder
          .addJavadoc("@return a page of entities\n")
          .addStatement("return service.findAll(pageable)");
      builder.addMethod(findAllBuilder.build());
    }

    addStandardOverrides(builder, model);

    model.getEndpointRelations().forEach(r -> r.addEndpoint(builder));

    return builder.build();
  }

  private static void addStandardOverrides(TypeSpec.Builder builder, JpaEntityModel model) {
    var idName = model.getJpaId().name();
    var idType = model.getJpaId().type();
    var entityType = model.getEntityType();

    // getById
    builder.addMethod(
        MethodSpec
            .methodBuilder("getById")
            .addAnnotation(AnnotationSpec.builder(GET_MAPPING).addMember("value", "$S", "/{" + idName + "}").build())
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation(Override.class)
            .returns(entityType)
            .addParameter(ParameterSpec.builder(idType, idName).addAnnotation(PATH_VARIABLE).build())
            .addStatement("return service.getById($L)", idName)
            .build()
    );

    // exists
    builder.addMethod(
        MethodSpec
            .methodBuilder("exists")
            .addAnnotation(
                AnnotationSpec
                    .builder(REQUEST_MAPPING)
                    .addMember("method", "$T.HEAD", REQUEST_METHOD)
                    .addMember("path", "$S", "/{" + idName + "}")
                    .build()
            )
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation(Override.class)
            .returns(TypeName.BOOLEAN)
            .addParameter(ParameterSpec.builder(idType, idName).addAnnotation(PATH_VARIABLE).build())
            .addStatement("return service.exists($L)", idName)
            .build()
    );

    // update
    builder.addMethod(
        MethodSpec
            .methodBuilder("update")
            .addAnnotation(AnnotationSpec.builder(PUT_MAPPING).addMember("value", "$S", "/{" + idName + "}").build())
            .addAnnotation(AnnotationSpec.builder(RESPONSE_STATUS).addMember("code", "$T.CREATED", HTTP_STATUS).build())
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation(Override.class)
            .returns(entityType)
            .addParameter(ParameterSpec.builder(idType, idName).addAnnotation(PATH_VARIABLE).build())
            .addParameter(ParameterSpec.builder(entityType, "entity").addAnnotation(VALID).addAnnotation(REQUEST_BODY).build())
            .addStatement("return service.update($L, entity)", idName)
            .build()
    );

    // delete
    builder.addMethod(
        MethodSpec
            .methodBuilder("delete")
            .addAnnotation(AnnotationSpec.builder(DELETE_MAPPING).addMember("value", "$S", "/{" + idName + "}").build())
            .addAnnotation(
                AnnotationSpec
                    .builder(RESPONSE_STATUS)
                    .addMember("code", "$T.NO_CONTENT", HTTP_STATUS)
                    .build()
            )
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation(Override.class)
            .returns(TypeName.VOID)
            .addParameter(ParameterSpec.builder(idType, idName).addAnnotation(PATH_VARIABLE).build())
            .addStatement("service.deleteById($L)", idName)
            .build()
    );
  }
}
