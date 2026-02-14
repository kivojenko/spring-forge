package com.kivojenko.spring.forge.jpa.utils;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;

/**
 * Utility for logging diagnostic messages during annotation processing.
 */
public final class LoggingUtils {
  /**
   * Prints an informational message.
   *
   * @param processingEnv the processing environment
   * @param element       the element to which the message pertains
   * @param message       the message to print
   */
  public static void info(ProcessingEnvironment processingEnv, Element element, String message) {
    processingEnv.getMessager().printNote(message, element);
  }

  /**
   * Prints a warning message.
   *
   * @param processingEnv the processing environment
   * @param element       the element to which the message pertains
   * @param message       the message to print
   */
  public static void warn(ProcessingEnvironment processingEnv, Element element, String message) {
    processingEnv.getMessager().printWarning(message, element);
  }

  /**
   * Prints an error message.
   *
   * @param processingEnv the processing environment
   * @param element       the element to which the message pertains
   * @param message       the message to print
   */
  public static void error(ProcessingEnvironment processingEnv, Element element, String message) {
    processingEnv.getMessager().printError(message, element);
  }
}
