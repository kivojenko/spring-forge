package com.kivojenko.spring.forge.jpa.model;

import com.kivojenko.spring.forge.annotation.*;
import com.kivojenko.spring.forge.jpa.utils.LoggingUtils;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.kivojenko.spring.forge.jpa.utils.StringUtils.capitalize;
import static java.beans.Introspector.decapitalize;


public class JpaEntityModelFactory {
    public static JpaEntityModel create(TypeElement entity, ProcessingEnvironment env, RoundEnvironment roundEnv) {
        var elements = env.getElementUtils();
        var entityType = ClassName.get(entity);
        var jpaId = JpaId.resolveId(entity);

        var types = env.getTypeUtils();
        var hasNameType = elements.getTypeElement("com.kivojenko.spring.forge.jpa.contract.HasName");
        var hasName = hasNameType != null && types.isAssignable(entity.asType(), hasNameType.asType());

        var packageName = resolvePackageName(entity);

        var repositoryPackage = resolveRepositoryPackageName(entity, env);
        var servicePackage = resolveServicePackageName(entity, env);
        var controllerPackage = resolveControllerPackageName(entity, env);

        if (!packageName.isBlank()) {
            repositoryPackage += "." + packageName;
            servicePackage += "." + packageName;
            controllerPackage += "." + packageName;
        }

        var wantsService = entity.getAnnotation(WithService.class) != null;
        var wantsController = entity.getAnnotation(WithRestController.class) != null;
        var wantsGetOrCreate = entity.getAnnotation(GetOrCreate.class) != null;

        if (wantsGetOrCreate && !hasName) {
            LoggingUtils.warn(env,
                    entity,
                    "Entity " +
                            entityType +
                            " is annotated with @WithGetOrCreate but does not implement HasName interface");
            wantsGetOrCreate = false;
        }
        if (wantsGetOrCreate && !wantsService) {
            wantsService = true;
        }

        return new JpaEntityModel(entity, entityType, jpaId, resolveEndpointRelations(entity, env),

                packageName, repositoryPackage, servicePackage, controllerPackage,

                hasName, wantsService, wantsController, wantsGetOrCreate,

                env, roundEnv);
    }


    private static List<EndpointRelation> resolveEndpointRelations(TypeElement entity, ProcessingEnvironment env) {
        var endpointRelations = new ArrayList<EndpointRelation>();

        for (var enclosed : entity.getEnclosedElements()) {
            EndpointRelation relation = null;
            if (enclosed instanceof VariableElement field) {
                relation = resolveFieldEndpointRelation(field, env);
            } else if (enclosed instanceof ExecutableElement getter) {
                relation = resolveGetterEndpointRelation(getter, env);
            }

            if (relation != null) endpointRelations.add(relation);

        }
        return endpointRelations;

    }

    private static EndpointRelation resolveFieldEndpointRelation(VariableElement field, ProcessingEnvironment env) {
        var withEndpoints = field.getAnnotation(WithEndpoints.class);
        if (withEndpoints == null) return null;

        var methodName = withEndpoints.getMethodName();
        if (methodName.isBlank()) methodName ="get" + capitalize(field.getSimpleName().toString());

        var path = withEndpoints.path();
        if (path.isBlank()) path =  field.getSimpleName().toString();

        if (!(field.asType() instanceof DeclaredType declaredType)) {
            LoggingUtils.warn(env, field, "@WithEndpoints can only be used on collection associations");
            return null;
        }

        var typeArgs = declaredType.getTypeArguments();
        if (typeArgs.size() != 1) {
            LoggingUtils.warn(env, field, "@WithEndpoints must be used on generic collections");
            return null;
        }

        var elementType = TypeName.get(typeArgs.getFirst());
        return new EndpointRelation(path,
                methodName,
                elementType,
                withEndpoints.read(),
                withEndpoints.add(),
                withEndpoints.remove());
    }

    private static EndpointRelation resolveGetterEndpointRelation(ExecutableElement getter, ProcessingEnvironment env) {
        var withEndpoints = getter.getAnnotation(WithGetEndpoint.class);
        if (withEndpoints == null) return null;
        var methodName = withEndpoints.getMethodName();
        if (methodName.isBlank()) methodName = getter.getSimpleName().toString();

        var path = withEndpoints.path();
        if (path.isBlank()) {
            path = getter.getSimpleName().toString();
            if (path.startsWith("get")) path = decapitalize(path.substring(3));

        }

        var returnType = getter.getReturnType();

        if (!(returnType instanceof DeclaredType declared)) {
            LoggingUtils.warn(env, getter, "@WithEndpoints can only be used on collection associations");
            return null;
        }

        var args = declared.getTypeArguments();
        if (args.size() != 1) return null;
        var elementType = TypeName.get(args.getFirst());
        return new EndpointRelation(path, methodName, elementType, true, false, false);
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
