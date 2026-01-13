package com.kivojenko.spring.forge.jpa.model.model;

import com.kivojenko.spring.forge.annotation.WithJpaRepository;
import com.kivojenko.spring.forge.annotation.WithRestController;
import com.kivojenko.spring.forge.annotation.WithService;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;
import java.util.Optional;

public record JpaEntityPackageNames(String packageName,
                                    String repositoryPackageName,
                                    String servicePackageName,
                                    String controllerPackageName) {

    public static JpaEntityPackageNames resolvePackageNames(TypeElement entity, ProcessingEnvironment env) {
        var packageName = resolvePackageName(entity);

        var repositoryPackage = resolveRepositoryPackageName(entity, env);
        var servicePackage = resolveServicePackageName(entity, env);
        var controllerPackage = resolveControllerPackageName(entity, env);

        if (!packageName.isBlank()) {
            repositoryPackage += "." + packageName;
            servicePackage += "." + packageName;
            controllerPackage += "." + packageName;
        }

        return new JpaEntityPackageNames(packageName, repositoryPackage, servicePackage, controllerPackage);
    }


    private static String resolvePackageName(TypeElement entity) {
        var withJpaRepository = entity.getAnnotation(WithJpaRepository.class);
        if (withJpaRepository != null && !withJpaRepository.packageName().isBlank()) {
            return withJpaRepository.packageName();
        }

        var withService = entity.getAnnotation(WithService.class);
        if (withService != null && !withService.packageName().isBlank()) {
            return withService.packageName();
        }

        var withRestController = entity.getAnnotation(WithRestController.class);
        if (withRestController != null && !withRestController.packageName().isBlank()) {
            return withRestController.packageName();
        }

        return "";
    }


    private static String resolveRepositoryPackageName(TypeElement entity, ProcessingEnvironment env) {
        return resolvePackageName(entity, env, "springforge.repository.package");
    }

    private static String resolveServicePackageName(TypeElement entity, ProcessingEnvironment env) {
        return resolvePackageName(entity, env, "springforge.service.package");
    }

    private static String resolveControllerPackageName(TypeElement entity, ProcessingEnvironment env) {
        return resolvePackageName(entity, env, "springforge.controller.package");
    }

    private static String resolvePackageName(TypeElement entity, ProcessingEnvironment env, String optionName) {
        var elements = env.getElementUtils();
        var packageName = elements.getPackageOf(entity).getQualifiedName().toString();

        return Optional.ofNullable(env.getOptions().get(optionName)).filter(s -> !s.isBlank()).orElse(packageName);
    }
}
