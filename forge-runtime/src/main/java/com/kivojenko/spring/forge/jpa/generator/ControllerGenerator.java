package com.kivojenko.spring.forge.jpa.generator;

import com.kivojenko.spring.forge.config.SpringForgeConfig;
import com.kivojenko.spring.forge.jpa.model.base.JpaEntityModel;
import com.squareup.javapoet.*;

import javax.lang.model.element.Modifier;

import static com.kivojenko.spring.forge.jpa.utils.ClassNameUtils.*;

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
    var superClass = ParameterizedTypeName.get(
        FORGE_CONTROLLER,
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

    if (model.getRequirements().getOrCreateAnnotation() != null) {
      var annotation = AnnotationSpec.builder(POST_MAPPING).addMember("value", "$S", "/get-or-create").build();

      var nameParam = ParameterSpec.builder(String.class, "name").addAnnotation(REQUEST_PARAM).build();
      var getOrCreate = MethodSpec
          .methodBuilder("getOrCreate")
          .addJavadoc("Retrieves an existing {@link $T} by name or creates it if it does not exist.\n", model.getEntityType())
          .addJavadoc("@param name the name of the entity\n")
          .addJavadoc("@return the retrieved or newly created entity\n")
          .addModifiers(Modifier.PUBLIC)
          .addAnnotation(annotation)
          .addParameter(nameParam)
          .returns(model.getEntityType())
          .addStatement("return service.getOrCreate(name)")
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
      var filterParam = ParameterSpec.builder(model.getFilterType(), "filter").build();
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

    model.getEndpointRelations().forEach(r -> r.addEndpoint(builder));

    return builder.build();
  }
}
