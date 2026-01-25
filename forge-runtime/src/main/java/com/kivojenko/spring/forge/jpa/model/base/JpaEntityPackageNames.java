package com.kivojenko.spring.forge.jpa.model.base;

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
 * @param packageName           the custom sub-package name
 * @param repositoryPackageName the package for repositories
 * @param servicePackageName    the package for services
 * @param controllerPackageName the package for controllers
 * @param filterPackageName     the package for filters
 */
public record JpaEntityPackageNames(
        String packageName,
        String repositoryPackageName,
        String servicePackageName,
        String controllerPackageName,
        String filterPackageName
)
{

    /**
     * Resolves package names for the given entity, considering configuration and annotations.
     *
     * @param entity the entity type element
     * @param e      the processing environment
     * @return the resolved package names
     */
    public static JpaEntityPackageNames resolvePackageNames(TypeElement entity, ProcessingEnvironment e) {
        if (!SpringForgeConfig.isLoaded) SpringForgeConfig.load(e);

        var packageName = resolvePackageName(entity);

        var repositoryPackage = resolvePackageName(entity, SpringForgeConfig.repositoryPackage, e);
        var servicePackage = resolvePackageName(entity, SpringForgeConfig.servicePackage, e);
        var controllerPackage = resolvePackageName(entity, SpringForgeConfig.controllerPackage, e);
        var filterPackage = resolvePackageName(entity, SpringForgeConfig.filterPackage, e);

        if (!packageName.isBlank()) {
            repositoryPackage += "." + packageName;
            servicePackage += "." + packageName;
            controllerPackage += "." + packageName;
            filterPackage += "." + packageName;
        }

        return new JpaEntityPackageNames(
                packageName,
                repositoryPackage,
                servicePackage,
                controllerPackage,
                filterPackage
        );
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
