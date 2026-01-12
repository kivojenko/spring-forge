package com.kivojenko.spring.forge.jpa.model;

import com.kivojenko.spring.forge.annotation.GetOrCreate;
import com.kivojenko.spring.forge.annotation.WithJpaRepository;
import com.kivojenko.spring.forge.annotation.WithRestController;
import com.kivojenko.spring.forge.annotation.WithService;
import com.kivojenko.spring.forge.jpa.utils.LoggingUtils;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import jakarta.persistence.MappedSuperclass;
import lombok.Builder;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.util.Optional;

import static java.beans.Introspector.decapitalize;

public record JpaEntityModel(TypeElement element,
                             ClassName entityType,
                             JpaId jpaId,

                             String packageName,
                             String repositoryPackageName,
                             String servicePackageName,
                             String controllerPackageName,

                             boolean hasName,
                             boolean wantsService,
                             boolean wantsController,
                             boolean wantsGetOrCreate,

                             ProcessingEnvironment env) {

    public String repositoryName() {
        return element.getSimpleName() + "ForgeRepository";
    }

    public String repositoryFqn() {
        return repositoryPackageName + "." + repositoryName();
    }

    public ClassName repositoryType() {
        return ClassName.get(repositoryPackageName, repositoryName());
    }

    public String serviceName() {
        return entityType.simpleName() + "ForgeService";
    }

    public String serviceFqn() {
        return servicePackageName + "." + serviceName();
    }

    public ClassName serviceType() {
        return ClassName.get(servicePackageName, serviceName());
    }

    public String controllerName() {
        return entityType.simpleName() + "ForgeController";
    }

    public String controllerFqn() {
        return controllerPackageName + "." + controllerName();
    }

    public String controllerPath() {
        return decapitalize(entityType.simpleName()) + "s";
    }


    public MethodSpec resolveCreateMethod() {

        if (hasBuilder()) {
            if (builderHasNameSetter()) {
                return createViaBuilder();
            }
            return createViaBuilderAndSetter();
        }


        if (hasEmptyCtor()) {
            return createViaEmptyCtorAndSetter();
        }

        if (hasStringCtor()) {
            return createViaCtor();
        }

        throw new IllegalStateException("Cannot generate getOrCreate for " +
                element().getSimpleName());
    }


    private MethodSpec createViaCtor() {
        return MethodSpec.methodBuilder("create")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PROTECTED)
                .returns(entityType())
                .addParameter(String.class, "name")
                .addStatement("return new $T(name)", entityType())
                .build();
    }

    private MethodSpec createViaBuilder() {
        return MethodSpec.methodBuilder("create")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PROTECTED)
                .returns(entityType())
                .addParameter(String.class, "name")
                .addStatement("return $T.builder().name(name).build()", entityType())
                .build();
    }

    private MethodSpec createViaEmptyCtorAndSetter() {
        return MethodSpec.methodBuilder("create")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PROTECTED)
                .returns(entityType())
                .addParameter(String.class, "name")
                .addStatement("var entity = new $T()", entityType())
                .addStatement("entity.setName(name)")
                .addStatement("return entity")
                .build();
    }

    private MethodSpec createViaBuilderAndSetter() {
        return MethodSpec.methodBuilder("create")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PROTECTED)
                .returns(entityType())
                .addParameter(String.class, "name")
                .addStatement("var entity = $T.builder().build()", entityType())
                .addStatement("entity.setName(name)")
                .addStatement("return entity")
                .build();
    }

    public boolean hasStringCtor() {
        return element().getEnclosedElements()
                .stream()
                .filter(e -> e.getKind() == ElementKind.CONSTRUCTOR)
                .map(ExecutableElement.class::cast)
                .filter(c -> c.getParameters().size() == 1)
                .anyMatch(c -> isString(c.getParameters().getFirst().asType()));
    }

    public boolean hasEmptyCtor() {
        return element().getEnclosedElements()
                .stream()
                .filter(e -> e.getKind() == ElementKind.CONSTRUCTOR)
                .map(ExecutableElement.class::cast)
                .anyMatch(c -> c.getParameters().isEmpty());
    }


    public boolean builderHasNameSetter() {
        return element().getEnclosedElements()
                .stream()
                .filter(c -> c.getKind() == ElementKind.FIELD)
                .anyMatch(c -> c.getSimpleName().contentEquals("name"));
    }


    public static boolean isString(TypeMirror type) {
        return type.getKind() == TypeKind.DECLARED &&
                ((TypeElement) ((DeclaredType) type).asElement()).getQualifiedName().contentEquals("java.lang.String");
    }

    public boolean hasBuilder() {
        return hasBuilderFactory() ||
                element().getAnnotation(Builder.class) != null ||
                element().getAnnotation(MappedSuperclass.class) != null;
    }

    public boolean hasBuilderFactory() {
        return element().getEnclosedElements()
                .stream()
                .filter(e -> e.getKind() == ElementKind.METHOD)
                .map(ExecutableElement.class::cast)
                .filter(m -> m.getModifiers().contains(Modifier.STATIC))
                .anyMatch(m -> m.getSimpleName().contentEquals("builder"));
    }

    public static JpaEntityModel of(TypeElement entity, ProcessingEnvironment env) {
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
            LoggingUtils.info(env, entity, "Enabling service for entity with @WithGetOrCreate annotation");
            wantsService = true;
        }

        return new JpaEntityModel(entity, entityType, jpaId,

                packageName, repositoryPackage, servicePackage, controllerPackage,

                hasName, wantsService, wantsController, wantsGetOrCreate,

                env);
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
