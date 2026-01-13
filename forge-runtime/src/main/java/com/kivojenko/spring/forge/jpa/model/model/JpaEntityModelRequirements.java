package com.kivojenko.spring.forge.jpa.model.model;

import com.kivojenko.spring.forge.annotation.GetOrCreate;
import com.kivojenko.spring.forge.annotation.WithRestController;
import com.kivojenko.spring.forge.annotation.WithService;
import com.kivojenko.spring.forge.jpa.utils.LoggingUtils;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;

public record JpaEntityModelRequirements(boolean hasName,
                                         boolean wantsService,
                                         boolean wantsController,
                                         boolean wantsGetOrCreate) {


    public static JpaEntityModelRequirements resolveRequirements(TypeElement entity, ProcessingEnvironment env) {
        var hasNameType = env.getElementUtils().getTypeElement("com.kivojenko.spring.forge.jpa.contract.HasName");
        var hasName = hasNameType != null && env.getTypeUtils().isAssignable(entity.asType(), hasNameType.asType());

        var wantsService = entity.getAnnotation(WithService.class) != null;
        var wantsController = entity.getAnnotation(WithRestController.class) != null;
        var wantsGetOrCreate = entity.getAnnotation(GetOrCreate.class) != null;

        if (wantsGetOrCreate && !hasName) {
            LoggingUtils.warn(env,
                    entity,
                    "Entity " +
                            entity.getSimpleName() +
                            " is annotated with @WithGetOrCreate but does not implement HasName");
            wantsGetOrCreate = false;
        }

        if (wantsGetOrCreate && !wantsService) {
            wantsService = true;
        }

        return new JpaEntityModelRequirements(hasName, wantsService, wantsController, wantsGetOrCreate);
    }

}
