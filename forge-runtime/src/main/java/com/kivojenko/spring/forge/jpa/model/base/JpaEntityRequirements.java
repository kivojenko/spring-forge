package com.kivojenko.spring.forge.jpa.model.base;

import com.kivojenko.spring.forge.annotation.GetOrCreate;
import com.kivojenko.spring.forge.annotation.WithJpaRepository;
import com.kivojenko.spring.forge.annotation.WithRestController;
import com.kivojenko.spring.forge.annotation.WithService;
import com.kivojenko.spring.forge.jpa.utils.LoggingUtils;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;

/**
 * Requirements and configuration flags for a JPA entity model.
 *
 * @param hasName               whether the entity has name property
 * @param repositoryAnnotation  annotation for repository configuration
 * @param serviceAnnotation     annotation for service configuration
 * @param controllerAnnotation  annotation for controller configuration
 * @param getOrCreateAnnotation annotation for "get or create" operation configuration
 */
public record JpaEntityRequirements(
        boolean hasName,
        WithJpaRepository repositoryAnnotation,
        WithService serviceAnnotation,
        WithRestController controllerAnnotation,
        GetOrCreate getOrCreateAnnotation
)
{
    /**
     * Resolves requirements for the given entity by checking its annotations and implemented interfaces.
     *
     * @param entity the entity type element
     * @param env    the processing environment
     * @return the resolved requirements
     */
    public static JpaEntityRequirements resolveRequirements(TypeElement entity, ProcessingEnvironment env) {
        var elements = env.getElementUtils();
        var types = env.getTypeUtils();

        var hasNameType = elements.getTypeElement("com.kivojenko.spring.forge.jpa.contract.HasName");
        var hasName = hasNameType != null && types.isAssignable(entity.asType(), hasNameType.asType());

        var controllerAnnotation = entity.getAnnotation(WithRestController.class);
        var serviceAnnotation = entity.getAnnotation(WithService.class);
        var repositoryAnnotation = entity.getAnnotation(WithJpaRepository.class);

        var getOrCreateAnnotation = entity.getAnnotation(GetOrCreate.class);

        if (getOrCreateAnnotation != null && !hasName) {
            LoggingUtils.warn(
                    env,
                    entity,
                    "Entity " +
                            entity.getSimpleName() +
                            " is annotated with @WithGetOrCreate but does not implement HasName"
            );
            getOrCreateAnnotation = null;
        }

        return new JpaEntityRequirements(
                hasName,
                repositoryAnnotation,
                serviceAnnotation,
                controllerAnnotation,
                getOrCreateAnnotation
        );
    }

    public boolean wantsRepository() {
        return repositoryAnnotation != null || wantsService() || wantsController();
    }

    /**
     * Determines if the repository for the entity should be abstract.
     * An abstract repository allows for custom implementation later.
     *
     * @return true if the repository should be abstract, false otherwise
     */
    public boolean wantsAbstractRepository() {
        return repositoryAnnotation != null && repositoryAnnotation.makeAbstract();
    }

    /**
     * Checks if a service should be generated for the entity.
     *
     * @return true if a service should be generated, false otherwise
     */
    public boolean wantsService() {
        return serviceAnnotation != null || getOrCreateAnnotation != null;
    }

    /**
     * Determines if the service for the entity should be abstract.
     * An abstract service allows for custom implementation later.
     *
     * @return true if the service should be abstract, false otherwise
     */
    public boolean wantsAbstractService() {
        return serviceAnnotation != null && serviceAnnotation.makeAbstract();
    }

    /**
     * Checks if a controller should be generated for the entity.
     *
     * @return true if a controller should be generated, false otherwise
     */
    public boolean wantsController() {
        return controllerAnnotation != null;
    }

    /**
     * Determines if the controller for the entity should be abstract.
     * An abstract controller allows for custom implementation later.
     *
     * @return true if the controller should be abstract, false otherwise
     */
    public boolean wantsAbstractController() {
        return controllerAnnotation != null && controllerAnnotation.makeAbstract();
    }

}
