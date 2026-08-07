package com.kivojenko.spring.forge.jpa.model.base;

import com.kivojenko.spring.forge.annotation.GetOrCreate;
import com.kivojenko.spring.forge.annotation.WithJpaRepository;
import com.kivojenko.spring.forge.annotation.WithRestController;
import com.kivojenko.spring.forge.annotation.WithService;
import com.kivojenko.spring.forge.jpa.utils.LoggingUtils;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.MirroredTypesException;
import javax.lang.model.type.TypeMirror;
import java.util.ArrayList;
import java.util.List;

/**
 * Requirements and configuration flags for a JPA entity model.
 *
 * @param hasName               whether the entity has name property
 * @param repositoryAnnotation  annotation for repository configuration
 * @param repositoryInterfaces  additional interfaces for the repository
 * @param serviceAnnotation     annotation for service configuration
 * @param controllerAnnotation  annotation for controller configuration
 * @param getOrCreateAnnotation annotation for "get or create" operation configuration
 */
public record JpaEntityRequirements(
        boolean hasName,
        WithJpaRepository repositoryAnnotation,
        List<TypeName> repositoryInterfaces,
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
        var repositoryInterfaces = resolveRepositoryInterfaces(entity, repositoryAnnotation, env);

        var getOrCreateAnnotation = entity.getAnnotation(GetOrCreate.class);

        if (getOrCreateAnnotation != null && !hasName) {
            LoggingUtils.error(
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
                repositoryInterfaces,
                serviceAnnotation,
                controllerAnnotation,
                getOrCreateAnnotation
        );
    }

    private static List<TypeName> resolveRepositoryInterfaces(TypeElement entity, WithJpaRepository repositoryAnnotation, ProcessingEnvironment env) {
        if (repositoryAnnotation == null) {
            return List.of();
        }
        List<TypeName> result = new ArrayList<>();
        var entityTypeName = TypeName.get(entity.asType());

        try {
            for (Class<?> clazz : repositoryAnnotation.interfaces()) {
                var typeElement = env.getElementUtils().getTypeElement(clazz.getCanonicalName());
                result.add(parameterizeIfGeneric(typeElement.asType(), entityTypeName));
            }
        } catch (MirroredTypesException mte) {
            for (TypeMirror mirror : mte.getTypeMirrors()) {
                result.add(parameterizeIfGeneric(mirror, entityTypeName));
            }
        }
        return result;
    }

    private static TypeName parameterizeIfGeneric(TypeMirror mirror, TypeName entityTypeName) {
        var typeName = TypeName.get(mirror);
        if (mirror instanceof DeclaredType declaredType) {
            var element = declaredType.asElement();
            if (element instanceof TypeElement typeElement && !typeElement.getTypeParameters().isEmpty()) {
                if (typeElement.getTypeParameters().size() == 1 && typeName instanceof ClassName className) {
                    return ParameterizedTypeName.get(className, entityTypeName);
                }
            }
        }
        return typeName;
    }

    public boolean wantsRepository() {
        return repositoryAnnotation != null || wantsService() || wantsController();
    }

    public boolean wantsAbstractRepository() {
        return repositoryAnnotation != null && repositoryAnnotation.makeAbstract();
    }

    public boolean wantsService() {
        return serviceAnnotation != null || getOrCreateAnnotation != null || wantsController();
    }

    public boolean wantsAbstractService() {
        return serviceAnnotation != null && serviceAnnotation.makeAbstract();
    }

    public boolean wantsController() {
        return controllerAnnotation != null;
    }

    public boolean wantsAbstractController() {
        return controllerAnnotation != null && controllerAnnotation.makeAbstract();
    }

}
