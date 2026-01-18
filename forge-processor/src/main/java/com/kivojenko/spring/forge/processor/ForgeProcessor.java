package com.kivojenko.spring.forge.processor;

import com.kivojenko.spring.forge.annotation.GetOrCreate;
import com.kivojenko.spring.forge.annotation.WithJpaRepository;
import com.kivojenko.spring.forge.annotation.WithRestController;
import com.kivojenko.spring.forge.annotation.WithService;
import com.kivojenko.spring.forge.jpa.generator.ControllerGenerator;
import com.kivojenko.spring.forge.jpa.generator.FilterGenerator;
import com.kivojenko.spring.forge.jpa.generator.RepositoryGenerator;
import com.kivojenko.spring.forge.jpa.generator.ServiceGenerator;
import com.kivojenko.spring.forge.jpa.factory.EndpointRelationFactory;
import com.kivojenko.spring.forge.jpa.factory.JpaEntityModelFactory;
import com.kivojenko.spring.forge.jpa.model.base.JpaEntityModel;
import com.kivojenko.spring.forge.jpa.utils.LoggingUtils;
import com.squareup.javapoet.JavaFile;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;


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
        FilterGenerator.setEnv(processingEnv);
        var entities = new HashSet<Element>();

        entities.addAll(roundEnv.getElementsAnnotatedWith(WithJpaRepository.class));
        entities.addAll(roundEnv.getElementsAnnotatedWith(WithService.class));
        entities.addAll(roundEnv.getElementsAnnotatedWith(GetOrCreate.class));
        entities.addAll(roundEnv.getElementsAnnotatedWith(WithRestController.class));

        var rootEntities = entities
                .stream()
                .filter(TypeElement.class::isInstance)
                .map(TypeElement.class::cast)
                .collect(Collectors.toSet());

        expandEntityGraph(rootEntities, processingEnv);
        rootEntities.forEach(JpaEntityModelFactory::get);

        var jpaEntities = JpaEntityModelFactory.getAll();

        jpaEntities.forEach(this::addRepository);
        jpaEntities.forEach(this::addService);
        jpaEntities.forEach(this::addController);
        jpaEntities.forEach(this::addFilter);

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
                var related = rel.getEntityModel().element();
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
        if (!model.requirements().wantsRepository() || alreadyExists(model.repositoryFqn())) return;

        var file = RepositoryGenerator.generateFile(model);
        tryWriteTo(file, model.element());
    }

    /**
     * Generates a service for the given entity model if requested and doesn't exist.
     *
     * @param model the entity model
     */
    private void addService(JpaEntityModel model) {
        if (!model.requirements().wantsService() || alreadyExists(model.serviceFqn())) return;

        var file = ServiceGenerator.generateFile(model);
        tryWriteTo(file, model.element());
    }

    /**
     * Generates a REST controller for the given entity model if requested and doesn't exist.
     *
     * @param model the entity model
     */
    private void addController(JpaEntityModel model) {
        if (!model.requirements().wantsController() || alreadyExists(model.controllerFqn())) return;

        var file = ControllerGenerator.generateFile(model);
        tryWriteTo(file, model.element());
    }

    private void addFilter(JpaEntityModel model) {
        if (model.getFilterableFields().isEmpty() || alreadyExists(model.filterFqn())) return;

        var file = FilterGenerator.generateFile(model);
        tryWriteTo(file, model.element());
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
     * @param file    the JavaPoet JavaFile
     * @param element the source element for error logging
     */
    private void tryWriteTo(JavaFile file, Element element) {
        try {
            file.writeTo(processingEnv.getFiler());
        } catch (Exception e) {
            LoggingUtils.error(processingEnv, element, e.getMessage());
        }
    }
}
