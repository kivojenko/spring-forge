package com.kivojenko.spring.forge.processor;

import com.kivojenko.spring.forge.annotation.HasJpaRepository;
import com.kivojenko.spring.forge.annotation.HasRestController;
import com.kivojenko.spring.forge.jpa.controller.JpaControllerGenerator;
import com.kivojenko.spring.forge.jpa.model.JpaEntityModel;
import com.kivojenko.spring.forge.jpa.repository.JpaRepositoryGenerator;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import java.util.HashSet;
import java.util.Set;

@SupportedAnnotationTypes("com.kivojenko.spring.forge.annotation.*")
@SupportedSourceVersion(SourceVersion.RELEASE_21)
@SupportedOptions({JpaForgeProcessor.REPOSITORY_PACKAGE_OPTION, JpaForgeProcessor.CONTROLLER_PACKAGE_OPTION})
public final class JpaForgeProcessor extends AbstractProcessor {
    public static final String REPOSITORY_PACKAGE_OPTION = "springforge.repository.package";
    public static final String CONTROLLER_PACKAGE_OPTION = "springforge.controller.package";

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        var entities = new HashSet<Element>();

        entities.addAll(roundEnv.getElementsAnnotatedWith(HasJpaRepository.class));
        entities.addAll(roundEnv.getElementsAnnotatedWith(HasRestController.class));

        entities.stream()
                .filter(TypeElement.class::isInstance)
                .map(TypeElement.class::cast)
                .distinct()
                .map(e -> JpaEntityModel.of(e, processingEnv))
                .forEach(this::process);
        return true;

    }

    private void process(JpaEntityModel model) {
        addRepository(model);

        if (model.wantsController()) {
            addController(model);
        }
    }


    private void addRepository(JpaEntityModel model) {
        if (repositoryAlreadyExists(model)) return;

        try {
            JpaRepositoryGenerator.generateFile(model).writeTo(processingEnv.getFiler());
        } catch (Exception e) {
            error(model.element(), e.getMessage());
        }
    }

    private boolean repositoryAlreadyExists(JpaEntityModel model) {
        return processingEnv.getElementUtils().getTypeElement(model.repositoryFqn()) != null;
    }

    private void addController(JpaEntityModel model) {
        if (controllerAlreadyExists(model)) return;

        try {
            JpaControllerGenerator.generateFile(model).writeTo(processingEnv.getFiler());
        } catch (Exception e) {
            error(model.element(), e.getMessage());
        }
    }

    private boolean controllerAlreadyExists(JpaEntityModel model) {
        return processingEnv.getElementUtils().getTypeElement(model.controllerFqn()) != null;
    }

    private void error(Element element, String message) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, message, element);
    }
}
