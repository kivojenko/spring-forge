package com.kivojenko.spring.forge.jpa.factory;

import com.kivojenko.spring.forge.annotation.endpoint.WithEndpoints;
import com.kivojenko.spring.forge.annotation.endpoint.WithGetEndpoint;
import com.kivojenko.spring.forge.jpa.model.relation.EndpointRelation;
import com.kivojenko.spring.forge.jpa.model.relation.manyToMany.AddManyToManyEndpointRelation;
import com.kivojenko.spring.forge.jpa.model.relation.manyToMany.ReadManyToManyEndpointRelation;
import com.kivojenko.spring.forge.jpa.model.relation.manyToOne.ReadManyToOneEndpointRelation;
import com.kivojenko.spring.forge.jpa.model.relation.oneToMany.AddOneToManyEndpointRelation;
import com.kivojenko.spring.forge.jpa.model.relation.oneToMany.ReadOneToManyEndpointRelation;
import com.kivojenko.spring.forge.jpa.model.relation.manyToOne.AddManyToOneEndpointRelation;
import com.kivojenko.spring.forge.jpa.model.relation.manyToOne.RemoveManyToOneEndpointRelation;
import com.kivojenko.spring.forge.jpa.model.relation.oneToMany.RemoveOneToManyEndpointRelation;
import com.kivojenko.spring.forge.jpa.model.relation.oneToOne.ReadOneToOneEndpointRelation;
import com.kivojenko.spring.forge.jpa.utils.LoggingUtils;
import jakarta.persistence.Embedded;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import org.jspecify.annotations.NonNull;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import java.util.ArrayList;
import java.util.List;

import static com.kivojenko.spring.forge.jpa.utils.StringUtils.capitalize;
import static java.beans.Introspector.decapitalize;

/**
 * Factory for resolving {@link EndpointRelation}s from entity elements.
 */
public class EndpointRelationFactory {

    /**
     * Resolves all endpoint relations for the given entity.
     * It scans for fields annotated with {@link WithEndpoints} and methods annotated with {@link WithGetEndpoint}.
     *
     * @param entity the entity type element
     * @param env    the processing environment
     * @return a list of resolved endpoint relations
     */
    public static List<EndpointRelation> resolve(TypeElement entity, ProcessingEnvironment env) {
        var endpointRelations = new ArrayList<EndpointRelation>();

        for (var enclosed : entity.getEnclosedElements()) {
            if (enclosed instanceof VariableElement field) {
                endpointRelations.addAll(resolveFieldEndpointRelation(field, env));
            } else if (enclosed instanceof ExecutableElement getter) {
                var relation = resolveGetterEndpointRelation(getter, env);
                if (relation != null) endpointRelations.add(relation);
            }

        }
        endpointRelations.forEach(r -> r.setEntityModel(JpaEntityModelFactory.get(entity)));
        return endpointRelations;

    }

    /**
     * Resolves all endpoint relations for the given field.
     *
     * @param field the field element
     * @param env   the processing environment
     * @return a list of resolved endpoint relations
     */
    private static @NonNull List<EndpointRelation> resolveFieldEndpointRelation(
            VariableElement field,
            ProcessingEnvironment env
    ) {
        var result = new ArrayList<EndpointRelation>();

        var withEndpoints = field.getAnnotation(WithEndpoints.class);
        if (withEndpoints == null) return result;

        var methodName = withEndpoints.getMethodName();
        if (methodName.isBlank()) methodName = "get" + capitalize(field.getSimpleName().toString());

        var path = withEndpoints.path();
        if (path.isBlank()) path = field.getSimpleName().toString();

        var embedded = field.getAnnotation(Embedded.class);

        var oneToMany = field.getAnnotation(OneToMany.class);
        if (oneToMany != null) {
            var mappedBy = oneToMany.mappedBy();
            if (mappedBy.isBlank()) {
                LoggingUtils.warn(
                        env,
                        field,
                        "@WithEndpoints.remove() can only be used on OneToMany associations with mappedBy"
                );
                oneToMany = null;
            }
        }
        var manyToOne = field.getAnnotation(ManyToOne.class);
        var manyToMany = field.getAnnotation(ManyToMany.class);

        TypeElement elementType;
        if (manyToOne != null || embedded != null) {
            elementType = (TypeElement) env.getTypeUtils().asElement(field.asType());
        } else {
            elementType = getElementTypeFromList(field.asType(), field, env);
        }

        if (withEndpoints.read()) {
            if (embedded != null) {
                result.add(ReadOneToOneEndpointRelation
                        .builder()
                        .path(path)
                        .field(field)
                        .methodName(methodName)
                        .targetEntityModel(JpaEntityModelFactory.get(elementType))
                        .build());
            }
            if (manyToMany != null) {
                result.add(ReadManyToManyEndpointRelation
                        .builder()
                        .path(path)
                        .field(field)
                        .methodName(methodName)
                        .fieldName(field.getSimpleName().toString())
                        .targetEntityModel(JpaEntityModelFactory.get(elementType))
                        .build());
            }
            if (oneToMany != null) {
                result.add(ReadOneToManyEndpointRelation
                        .builder()
                        .path(path)
                        .field(field)
                        .methodName(methodName)
                        .targetEntityModel(JpaEntityModelFactory.get(elementType))
                        .build());
            }
            if (manyToOne != null) {
                result.add(ReadManyToOneEndpointRelation
                        .builder()
                        .path(path)
                        .field(field)
                        .methodName(methodName)
                        .targetEntityModel(JpaEntityModelFactory.get(elementType))
                        .build());
            }
        }

        if (withEndpoints.add()) {
            if (oneToMany != null) {
                result.add(AddOneToManyEndpointRelation
                        .builder()
                        .path(path)
                        .field(field)
                        .methodName(methodName)
                        .fieldName(field.getSimpleName().toString())
                        .mappedBy(oneToMany.mappedBy())
                        .targetEntityModel(JpaEntityModelFactory.get(elementType))
                        .build());
            }
            if (manyToOne != null) {
                result.add(AddManyToOneEndpointRelation
                        .builder()
                        .path(path)
                        .field(field)
                        .methodName(methodName)
                        .fieldName(field.getSimpleName().toString())
                        .targetEntityModel(JpaEntityModelFactory.get(elementType))
                        .build());
            }
            if (manyToMany != null) {
                result.add(AddManyToManyEndpointRelation
                        .builder()
                        .path(path)
                        .field(field)
                        .methodName(methodName)
                        .fieldName(field.getSimpleName().toString())
                        .targetEntityModel(JpaEntityModelFactory.get(elementType))
                        .build());
            }
        }

        if (withEndpoints.remove()) {
            if (oneToMany != null) {
                result.add(RemoveOneToManyEndpointRelation
                        .builder()
                        .path(path)
                        .field(field)
                        .methodName(methodName)
                        .fieldName(field.getSimpleName().toString())
                        .targetEntityModel(JpaEntityModelFactory.get(elementType))
                        .mappedBy(oneToMany.mappedBy())
                        .build());
            }

            if (manyToOne != null) {
                result.add(RemoveManyToOneEndpointRelation
                        .builder()
                        .path(path)
                        .field(field)
                        .methodName(methodName)
                        .fieldName(field.getSimpleName().toString())
                        .targetEntityModel(JpaEntityModelFactory.get(elementType))
                        .build());
            }
        }

        return result;
    }


    /**
     * Resolves an endpoint relation for the given getter method.
     *
     * @param getter the getter method element
     * @param env    the processing environment
     * @return the resolved endpoint relation, or null if none
     */
    private static EndpointRelation resolveGetterEndpointRelation(ExecutableElement getter, ProcessingEnvironment env) {
        var withGetEndpoint = getter.getAnnotation(WithGetEndpoint.class);
        if (withGetEndpoint == null) return null;

        var path = withGetEndpoint.path();

        if (path.isBlank()) {
            path = getter.getSimpleName().toString();
            if (getter.getSimpleName().toString().startsWith("get")) {
                path = decapitalize(path.substring(3));
            }
        }
        var elementType = getElementTypeFromList(getter.getReturnType(), getter, env);

        return ReadOneToManyEndpointRelation
                .builder()
                .path(path)
                .methodName(getter.getSimpleName().toString())
                .targetEntityModel(JpaEntityModelFactory.get(elementType))
                .build();
    }

    /**
     * Extracts the element type from a collection type (e.g., List<ElementType> -> ElementType).
     *
     * @param returnType the type mirror to extract from
     * @param element    the element associated with the type (for logging)
     * @param env        the processing environment
     * @return the extracted type element, or null if it's not a valid collection
     */
    private static TypeElement getElementTypeFromList(
            TypeMirror returnType,
            Element element,
            ProcessingEnvironment env
    ) {

        if (!(returnType instanceof DeclaredType declaredReturnType)) {
            LoggingUtils.warn(env, element, "Generating endpoints can only be used on declared return types");
            return null;
        }

        var typeArgs = declaredReturnType.getTypeArguments();
        if (typeArgs.size() != 1) {
            LoggingUtils.warn(env, element, "Generating endpoints must return a generic collection");
            return null;
        }

        if (!(typeArgs.getFirst() instanceof DeclaredType elementDeclaredType)) {
            LoggingUtils.warn(env, element, "Generating endpoints collection element type must be a declared type");
            return null;
        }

        var asElement = elementDeclaredType.asElement();
        if (!(asElement instanceof TypeElement elementType)) {
            LoggingUtils.warn(env, element, "Generating endpoints collection element is not a TypeElement");
            return null;
        }

        return elementType;
    }

}
