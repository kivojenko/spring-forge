package com.kivojenko.spring.forge.processor;

import com.kivojenko.spring.forge.annotation.GetOrCreate;
import com.kivojenko.spring.forge.annotation.WithJpaRepository;
import com.kivojenko.spring.forge.annotation.WithRestController;
import com.kivojenko.spring.forge.annotation.WithService;
import com.kivojenko.spring.forge.config.SpringForgeConfig;
import com.kivojenko.spring.forge.jpa.factory.EndpointRelationResolver;
import com.kivojenko.spring.forge.jpa.factory.JpaEntityModelFactory;
import com.kivojenko.spring.forge.jpa.generator.ControllerGenerator;
import com.kivojenko.spring.forge.jpa.generator.FilterGenerator;
import com.kivojenko.spring.forge.jpa.generator.RepositoryGenerator;
import com.kivojenko.spring.forge.jpa.generator.ServiceGenerator;
import com.kivojenko.spring.forge.jpa.model.base.JpaEntityModel;
import com.kivojenko.spring.forge.jpa.utils.LoggingUtils;
import com.squareup.javapoet.JavaFile;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;


/**
 * Annotation processor for generating JPA repositories, services, and REST controllers.
 * It processes entities annotated with {@link WithJpaRepository}, {@link WithService},
 * {@link WithRestController}, or {@link GetOrCreate}.
 */
@SupportedAnnotationTypes("com.kivojenko.spring.forge.annotation.*")
@SupportedSourceVersion(SourceVersion.RELEASE_21)
public final class ForgeProcessor extends AbstractProcessor {

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    if (annotations.isEmpty()) return false;
    if (!SpringForgeConfig.isLoaded()) SpringForgeConfig.load(processingEnv);

    JpaEntityModelFactory.setEnv(processingEnv);
    var annotationTypes = Set.of(
        WithJpaRepository.class,
        WithService.class,
        GetOrCreate.class,
        WithRestController.class
    );
    Set<TypeElement> rootEntities = new HashSet<>();

    for (var annotation : annotationTypes) {
      for (var element : roundEnv.getElementsAnnotatedWith(annotation)) {
        if (element instanceof TypeElement type) {
          rootEntities.add(type);
        }
      }
    }
    expandEntityGraph(rootEntities, processingEnv);
    rootEntities.forEach(JpaEntityModelFactory::get);

    var jpaEntities = JpaEntityModelFactory.getAll();

    jpaEntities.forEach(this::addFilter);
    jpaEntities.forEach(this::addRepository);
    jpaEntities.forEach(this::addService);
    jpaEntities.forEach(this::addController);

    return true;
  }

  /**
   * Expands the entity graph by following relationships defined on the root entities.
   * This ensures that related entities also have models generated if they are part of an endpoint.
   *
   * @param roots the set of initial entity type elements
   * @param env   the processing environment
   */
  void expandEntityGraph(Set<TypeElement> roots, ProcessingEnvironment env) {
    var result = new HashSet<TypeElement>();

    var queue = new ArrayDeque<>(roots);

    while (!queue.isEmpty()) {
      var entity = queue.poll();
      if (!result.add(entity)) continue;

      var relations = EndpointRelationResolver.resolve(entity, env);

      for (var rel : relations) {
        var related = rel.getEntityModel().getElement();
        if (related != null) {
          queue.add(related);
        }
      }
    }
  }


  private void addRepository(JpaEntityModel model) {
    if (!model.getRequirements().wantsRepository() || alreadyExists(model.getRepositoryFqn())) return;

    try {
      var file = RepositoryGenerator.generateFile(model);
      tryWriteTo(file);
    } catch (Exception e) {
      LoggingUtils.error(
          processingEnv,
          model.getElement(),
          "Failed to generate repository: " + e.getMessage() + Arrays.toString(e.getStackTrace())
      );
    }
  }

  private void addService(JpaEntityModel model) {
    if (!model.getRequirements().wantsService() || alreadyExists(model.getServiceFqn())) return;

    try {
      var file = ServiceGenerator.generateFile(model);
      tryWriteTo(file);
    } catch (Exception e) {
      LoggingUtils.error(
          processingEnv,
          model.getElement(),
          "Failed to generate service: " + e.getMessage() + Arrays.toString(e.getStackTrace())
      );
    }
  }

  private void addController(JpaEntityModel model) {
    if (!model.getRequirements().wantsController() || alreadyExists(model.getControllerFqn())) return;

    try {
      var file = ControllerGenerator.generateFile(model);
      tryWriteTo(file);
    } catch (Exception e) {
      LoggingUtils.error(
          processingEnv,
          model.getElement(),
          "Failed to generate controller: " + e.getMessage() + Arrays.toString(e.getStackTrace())
      );
    }
  }

  private void addFilter(JpaEntityModel model) {
    if (!model.wantsFilter() || alreadyExists(model.getFilterFqn())) return;

    try {
      var file = FilterGenerator.generateFile(model);
      tryWriteTo(file);
    } catch (Exception e) {
      LoggingUtils.error(
          processingEnv,
          model.getElement(),
          "Failed to generate filter: " + e.getMessage() + Arrays.toString(e.getStackTrace())
      );
    }
  }

  private boolean alreadyExists(String fqn) {
    return processingEnv.getElementUtils().getTypeElement(fqn) != null;
  }

  private void tryWriteTo(JavaFile file) {
    try {
      file.writeTo(processingEnv.getFiler());
    } catch (javax.annotation.processing.FilerException ignored) {
    } catch (Exception e) {
      LoggingUtils.error(processingEnv, null, e.getMessage());
    }
  }
}
