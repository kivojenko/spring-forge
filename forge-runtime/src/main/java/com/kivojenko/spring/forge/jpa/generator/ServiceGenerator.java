package com.kivojenko.spring.forge.jpa.generator;

import com.kivojenko.spring.forge.jpa.model.base.JpaEntityModel;
import com.squareup.javapoet.*;

import javax.lang.model.element.Modifier;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static com.kivojenko.spring.forge.jpa.utils.ClassNameUtils.*;

/**
 * Generator for Spring services.
 */
public final class ServiceGenerator {


    /**
     * Generates a {@link JavaFile} containing the service for the given model.
     *
     * @param model the entity model
     * @return the generated Java file
     */
    public static JavaFile generateFile(JpaEntityModel model) {
        return JavaFile.builder(model.getPackages().servicePackageName(), generate(model)).build();
    }

    /**
     * Generates the {@link TypeSpec} for the service.
     *
     * @param model the entity model
     * @return the type specification
     */
    public static TypeSpec generate(JpaEntityModel model) {
        var superClass = ParameterizedTypeName.get(
                FORGE_SERVICE,
                model.getEntityType(),
                model.getJpaId().type(),
                model.getRepositoryType()
        );
        var builder = TypeSpec
                .classBuilder(model.getServiceName())
                .addModifiers(Modifier.PUBLIC)
                .superclass(superClass)
                .addMethod(model.setIdMethod());

        if (model.getRequirements().wantsAbstractService()) {
            builder.addModifiers(Modifier.ABSTRACT);
        } else {
            builder.addAnnotation(SERVICE);
        }

        if (model.getRequirements().hasName()) {
            var create = MethodSpec
                    .methodBuilder("create")
                    .addAnnotation(TRANSACTIONAL)
                    .addModifiers(Modifier.PUBLIC)
                    .returns(model.getEntityType())
                    .addParameter(model.getEntityType(), "entity")
                    .beginControlFlow("if (repository.existsByName(entity.getName()))")
                    .addStatement(
                            "throw new $T($S + entity.getName() + $S)",
                            IllegalArgumentException.class,
                            "Entity with name ",
                            " already exists"
                    )
                    .endControlFlow()
                    .addStatement("return super.create(entity)")
                    .build();
            builder.addMethod(create);

            if (model.getRequirements().getOrCreateAnnotation() != null) {
                var getOrCreate = MethodSpec
                        .methodBuilder("getOrCreate")
                        .addModifiers(Modifier.PUBLIC)
                        .addAnnotation(TRANSACTIONAL)
                        .returns(model.getEntityType())
                        .addParameter(String.class, "name")
                        .addStatement("return repository.findByNameIgnoreCase(name)" +
                                ".orElseGet(() -> createSafely(name))")
                        .build();

                MethodSpec createSafely = MethodSpec
                        .methodBuilder("createSafely")
                        .addModifiers(Modifier.PRIVATE)
                        .returns(model.getEntityType())
                        .addParameter(String.class, "name")
                        .beginControlFlow("try")
                        .addStatement("return repository.save(create(name))")
                        .nextControlFlow("catch ($T e)", DATA_INTEGRITY_VIOLATION_EXCEPTION)
                        .addStatement("return repository.findByNameIgnoreCase(name).orElseThrow()")
                        .endControlFlow()
                        .build();

                builder.addMethod(model.nameCreateMethod()).addMethod(getOrCreate).addMethod(createSafely);
            }
        }


        if (model.wantsFilter()) {
            var pageableParam = ParameterSpec.builder(PAGEABLE, "pageable").build();
            var filterParam = ParameterSpec.builder(model.getFilterType(), "filter").build();
            var findAllPagedFiltered = MethodSpec
                    .methodBuilder("findAll")
                    .addModifiers(Modifier.PUBLIC)
                    .returns(ParameterizedTypeName.get(PAGE, model.getEntityType()))
                    .addParameter(pageableParam)
                    .addParameter(filterParam)
                    .addStatement("return repository.findAll(filter.toPredicate(), pageable)")
                    .build();


            var findAllFiltered = MethodSpec
                    .methodBuilder("findAll")
                    .addModifiers(Modifier.PUBLIC)
                    .returns(ParameterizedTypeName.get(ARRAY_LIST, model.getEntityType()))
                    .addParameter(filterParam)
                    .addStatement("var result = repository.findAll(filter.toPredicate())")
                    .addStatement(
                            "return $T.stream(result.spliterator(), false).collect($T.toCollection($T::new))",
                            ClassName.get(StreamSupport.class),
                            ClassName.get(Collectors.class),
                            ARRAY_LIST
                    )
                    .build();

            builder.addMethod(findAllPagedFiltered).addMethod(findAllFiltered);
        }

        model.getEndpointRelations().forEach(r -> r.addMethod(builder));
        return builder.build();
    }
}
