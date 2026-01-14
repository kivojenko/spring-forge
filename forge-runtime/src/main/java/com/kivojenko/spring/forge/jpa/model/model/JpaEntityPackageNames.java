package com.kivojenko.spring.forge.jpa.model.model;

import com.kivojenko.spring.forge.annotation.WithJpaRepository;
import com.kivojenko.spring.forge.annotation.WithRestController;
import com.kivojenko.spring.forge.annotation.WithService;
import com.kivojenko.spring.forge.config.SpringForgeConfig;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;
import java.util.Optional;

/**
 * Package names for generated classes related to a JPA entity.
 *
 * @param packageName the custom sub-package name
 * @param repositoryPackageName the package for repositories
 * @param servicePackageName the package for services
 * @param controllerPackageName the package for controllers
 */
public record JpaEntityPackageNames(String packageName,
                                    String repositoryPackageName,
                                    String servicePackageName,
                                    String controllerPackageName) {

    /**
     * Resolves package names for the given entity, considering configuration and annotations.
     *
     * @param entity the entity type element
     * @param config the Spring Forge configuration
     * @param e the processing environment
     * @return the resolved package names
     */
    public static JpaEntityPackageNames resolvePackageNames(TypeElement entity,
                                                            SpringForgeConfig config,
                                                            ProcessingEnvironment e) {
        var packageName = resolvePackageName(entity);

        var repositoryPackage = resolvePackageName(entity, config.getRepositoryPackage(), e);
        var servicePackage = resolvePackageName(entity, config.getServicePackage(), e);
        var controllerPackage = resolvePackageName(entity, config.getControllerPackage(), e);

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

    private static String resolvePackageName(TypeElement entity, String defaultPackageName, ProcessingEnvironment env) {
        var elements = env.getElementUtils();
        var packageName = elements.getPackageOf(entity).getQualifiedName().toString();

        return Optional.ofNullable(defaultPackageName).filter(s -> !s.isBlank()).orElse(packageName);
    }
}
