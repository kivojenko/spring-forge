package com.kivojenko.spring.forge.jpa.model.model;

import com.kivojenko.spring.forge.annotation.GetOrCreate;
import com.kivojenko.spring.forge.annotation.WithRestController;
import com.kivojenko.spring.forge.annotation.WithService;
import com.kivojenko.spring.forge.jpa.utils.LoggingUtils;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;

/**
 * Requirements and configuration flags for a JPA entity model.
 *
 * @param hasName                    whether the entity has name property
 * @param wantsService               whether a service should be generated
 * @param wantsAbstractController    whether an abstract controller should be generated
 * @param wantsImplementedController whether an implemented controller should be generated
 * @param wantsGetOrCreate           whether a "get or create" operation should be generated
 */
public record JpaEntityRequirements(boolean hasName,
                                    boolean wantsService,
                                    boolean wantsAbstractController,
                                    boolean wantsImplementedController,
                                    boolean wantsGetOrCreate) {
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

        var wantsAbstractController = controllerAnnotation != null && controllerAnnotation.makeAbstract();
        var wantsImplementedController = controllerAnnotation != null && !controllerAnnotation.makeAbstract();

        var wantsService = entity.getAnnotation(WithService.class) != null;

        var wantsGetOrCreate = entity.getAnnotation(GetOrCreate.class) != null;

        if (wantsGetOrCreate && !hasName) {
            LoggingUtils.warn(env, entity, "Entity " +
                    entity.getSimpleName() +
                    " is annotated with @WithGetOrCreate but does not implement HasName");
            wantsGetOrCreate = false;
        }

        if (wantsGetOrCreate && !wantsService) {
            wantsService = true;
        }

        return new JpaEntityRequirements(hasName, wantsService, wantsAbstractController, wantsImplementedController,
                wantsGetOrCreate);
    }

    /**
     * Checks if a controller should be generated for the entity.
     *
     * @return true if a controller should be generated, false otherwise
     */
    public boolean wantsController() {
        return wantsAbstractController || wantsImplementedController;
    }

}
