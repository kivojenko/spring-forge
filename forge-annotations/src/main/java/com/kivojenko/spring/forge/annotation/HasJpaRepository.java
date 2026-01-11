package com.kivojenko.spring.forge.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@ForgeTargetPackage
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface HasJpaRepository {
    String packageName() default "";
}
