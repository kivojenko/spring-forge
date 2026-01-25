package com.kivojenko.spring.forge.processor;

import com.kivojenko.spring.forge.annotation.GetOrCreate;
import com.kivojenko.spring.forge.annotation.WithJpaRepository;
import com.kivojenko.spring.forge.annotation.WithRestController;
import com.kivojenko.spring.forge.annotation.WithService;
import com.kivojenko.spring.forge.jpa.factory.EndpointRelationFactory;
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

            var relations = EndpointRelationFactory.resolve(entity, env);

            for (var rel : relations) {
                var related = rel.getEntityModel().getElement();
                if (related != null) {
                    queue.add(related);
                }
            }
        }
    }


    /**
     * Generates a repository for the given entity model if it doesn't exist.
     *
     * @param model the entity model
     */
    private void addRepository(JpaEntityModel model) {
        if (!model.getRequirements().wantsRepository() || alreadyExists(model.getRepositoryFqn())) return;

        try {
            var file = RepositoryGenerator.generateFile(model);
            tryWriteTo(file);
        } catch (Exception e) {
            LoggingUtils.error(processingEnv, model.getElement(), "Failed to generate repository: " + e.getMessage());
        }
    }

    /**
     * Generates a service for the given entity model if requested and doesn't exist.
     *
     * @param model the entity model
     */
    private void addService(JpaEntityModel model) {
        if (!model.getRequirements().wantsService() || alreadyExists(model.getServiceFqn())) return;

        try {
            var file = ServiceGenerator.generateFile(model);
            tryWriteTo(file);
        } catch (Exception e) {
            LoggingUtils.error(processingEnv, model.getElement(), "Failed to generate service: " + e.getMessage());
        }
    }

    /**
     * Generates a REST controller for the given entity model if requested and doesn't exist.
     *
     * @param model the entity model
     */
    private void addController(JpaEntityModel model) {
        if (!model.getRequirements().wantsController() || alreadyExists(model.getControllerFqn())) return;

        try {
            var file = ControllerGenerator.generateFile(model);
            tryWriteTo(file);
        } catch (Exception e) {
            LoggingUtils.error(processingEnv, model.getElement(), "Failed to generate controller: " + e.getMessage());
        }
    }

    /**
     * Generates a filter for the given entity model if requested and doesn't exist.
     *
     * @param model the entity model
     */
    private void addFilter(JpaEntityModel model) {
        if (!model.wantsFilter() || alreadyExists(model.getFilterFqn())) return;

        try {
            var file = FilterGenerator.generateFile(model);
            tryWriteTo(file);
        } catch (Exception e) {
            LoggingUtils.error(processingEnv, model.getElement(), "Failed to generate filter: " + e.getMessage());
        }
    }

    /**
     * Checks if a type with the given fully qualified name already exists.
     *
     * @param fqn the fully qualified name to check
     * @return true if exists, false otherwise
     */
    private boolean alreadyExists(String fqn) {
        return processingEnv.getElementUtils().getTypeElement(fqn) != null;
    }

    /**
     * Attempts to write the generated Java file using the filer.
     *
     * @param file the JavaPoet JavaFile
     */
    private void tryWriteTo(JavaFile file) {
        try {
            file.writeTo(processingEnv.getFiler());
        } catch (javax.annotation.processing.FilerException ignored) {
        } catch (Exception e) {
            LoggingUtils.error(processingEnv, null, e.getMessage());
        }
    }
}
