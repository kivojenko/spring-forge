package com.kivojenko.spring.forge.processor;

import com.kivojenko.spring.forge.annotation.GetOrCreate;
import com.kivojenko.spring.forge.annotation.WithJpaRepository;
import com.kivojenko.spring.forge.annotation.WithRestController;
import com.kivojenko.spring.forge.annotation.WithService;
import com.kivojenko.spring.forge.jpa.generator.JpaControllerGenerator;
import com.kivojenko.spring.forge.jpa.generator.JpaRepositoryGenerator;
import com.kivojenko.spring.forge.jpa.generator.JpaServiceGenerator;
import com.kivojenko.spring.forge.jpa.model.JpaEntityModel;
import com.kivojenko.spring.forge.jpa.model.JpaEntityModelFactory;
import com.kivojenko.spring.forge.jpa.utils.LoggingUtils;
import com.squareup.javapoet.JavaFile;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import java.util.HashSet;
import java.util.Set;


@SupportedAnnotationTypes("com.kivojenko.spring.forge.annotation.*")
@SupportedSourceVersion(SourceVersion.RELEASE_21)
@SupportedOptions({JpaForgeProcessor.REPOSITORY_PACKAGE_OPTION,
        JpaForgeProcessor.SERVICE_PACKAGE_OPTION,
        JpaForgeProcessor.CONTROLLER_PACKAGE_OPTION})
public final class JpaForgeProcessor extends AbstractProcessor {
    public static final String REPOSITORY_PACKAGE_OPTION = "springforge.repository.package";
    public static final String SERVICE_PACKAGE_OPTION = "springforge.service.package";
    public static final String CONTROLLER_PACKAGE_OPTION = "springforge.controller.package";

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        var entities = new HashSet<Element>();

        entities.addAll(roundEnv.getElementsAnnotatedWith(WithJpaRepository.class));
        entities.addAll(roundEnv.getElementsAnnotatedWith(WithService.class));
        entities.addAll(roundEnv.getElementsAnnotatedWith(GetOrCreate.class));
        entities.addAll(roundEnv.getElementsAnnotatedWith(WithRestController.class));

        entities.stream()
                .filter(TypeElement.class::isInstance)
                .map(TypeElement.class::cast)
                .distinct()
                .map(e -> JpaEntityModelFactory.create(e, processingEnv, roundEnv))
                .forEach(this::process);
        return true;
    }

    private void process(JpaEntityModel model) {
        addRepository(model);
        addService(model);
        addController(model);
    }

    private void addRepository(JpaEntityModel model) {
        if (alreadyExists(model.repositoryFqn())) return;

        var file = JpaRepositoryGenerator.generateFile(model);
        tryWriteTo(file, model.element());
    }

    private void addService(JpaEntityModel model) {
        if (!model.wantsService() || alreadyExists(model.serviceFqn())) return;

        var file = JpaServiceGenerator.generateFile(model);
        tryWriteTo(file, model.element());
    }

    private void addController(JpaEntityModel model) {
        if (!model.wantsController() || alreadyExists(model.controllerFqn())) return;

        var file = JpaControllerGenerator.generateFile(model);
        tryWriteTo(file, model.element());
    }

    private boolean alreadyExists(String fqn) {
        return processingEnv.getElementUtils().getTypeElement(fqn) != null;
    }

    private void tryWriteTo(JavaFile file, Element element) {
        try {
            file.writeTo(processingEnv.getFiler());
        } catch (Exception e) {
            LoggingUtils.error(processingEnv, element, e.getMessage());
        }
    }
}
