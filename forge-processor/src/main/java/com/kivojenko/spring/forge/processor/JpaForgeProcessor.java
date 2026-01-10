package com.kivojenko.spring.forge.processor;

import com.kivojenko.spring.forge.annotation.HasJpaRepository;
import com.kivojenko.spring.forge.jpa.JpaEntityModel;
import com.kivojenko.spring.forge.jpa.repository.JpaRepositoryGenerator;
import com.squareup.javapoet.JavaFile;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.util.Set;

@SupportedAnnotationTypes("com.kivojenko.spring.forge.annotation.HasJpaRepository")
@SupportedSourceVersion(SourceVersion.RELEASE_21)
public final class JpaForgeProcessor extends AbstractProcessor {

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        roundEnv.getElementsAnnotatedWith(HasJpaRepository.class)
                .stream()
                .filter(TypeElement.class::isInstance)
                .map(TypeElement.class::cast)
                .forEach(this::addRepository);

        return true;
    }


    private void addRepository(TypeElement entity) {
        var model = JpaEntityModel.of(entity, processingEnv);
        if (repositoryAlreadyExists(processingEnv.getElementUtils(), model)) return;

        try {
            writeRepository(model);
        } catch (Exception e) {
            error(entity, e.getMessage());
        }
    }

    private void writeRepository(JpaEntityModel model) throws IOException {
        var repository = JpaRepositoryGenerator.generate(model);

        JavaFile.builder(model.packageName(), repository).build().writeTo(processingEnv.getFiler());
    }

    private boolean repositoryAlreadyExists(Elements elements, JpaEntityModel model) {
        return elements.getTypeElement(model.repositoryFqn()) != null;
    }


    private void error(Element element, String message) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, message, element);
    }
}
