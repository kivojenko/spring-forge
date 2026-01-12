package com.kivojenko.spring.forge.jpa.utils;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.tools.Diagnostic;

public class LoggingUtils {
    public static void info(ProcessingEnvironment processingEnv, Element element, String message) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, message, element);
    }

    public static void warn(ProcessingEnvironment processingEnv, Element element, String message) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.WARNING, message, element);
    }

    public static void error(ProcessingEnvironment processingEnv, Element element, String message) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, message, element);
    }
}
