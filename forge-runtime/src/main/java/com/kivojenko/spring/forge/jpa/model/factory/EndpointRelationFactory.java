package com.kivojenko.spring.forge.jpa.model.factory;

import com.kivojenko.spring.forge.annotation.endpoint.WithEndpoints;
import com.kivojenko.spring.forge.annotation.endpoint.WithGetEndpoint;
import com.kivojenko.spring.forge.jpa.model.relation.EndpointRelation;
import com.kivojenko.spring.forge.jpa.model.relation.ReadEndpointRelation;
import com.kivojenko.spring.forge.jpa.model.relation.RemoveEndpointOneToManyRelation;
import com.kivojenko.spring.forge.jpa.utils.LoggingUtils;
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

public class EndpointRelationFactory {

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
        endpointRelations.forEach(r -> r.setEntityModel(JpaEntityModelFactory.get(entity, env)));
        return endpointRelations;

    }

    private static @NonNull List<EndpointRelation> resolveFieldEndpointRelation(VariableElement field,
                                                                                ProcessingEnvironment env) {
        var result = new ArrayList<EndpointRelation>();

        var withEndpoints = field.getAnnotation(WithEndpoints.class);
        if (withEndpoints == null) return result;

        var methodName = withEndpoints.getMethodName();
        if (methodName.isBlank()) methodName = "get" + capitalize(field.getSimpleName().toString());

        var path = withEndpoints.path();
        if (path.isBlank()) path = field.getSimpleName().toString();

        var elementType = getElementType(field.asType(), field, env);


        if (withEndpoints.read()) {
            result.add(ReadEndpointRelation.builder()
                    .path(path)
                    .methodName(methodName)
                    .targetEntityModel(JpaEntityModelFactory.get(elementType, env))
                    .build());
        }

        if (withEndpoints.remove()) {
            var oneToMany = field.getAnnotation(OneToMany.class);
            if (oneToMany != null) {
                var mappedBy = oneToMany.mappedBy();
                if (mappedBy.isBlank()) {
                    LoggingUtils.warn(env,
                            field,
                            "@WithEndpoints.remove() can only be used on associations with mappedBy");
                } else {
                    result.add(RemoveEndpointOneToManyRelation.builder()
                            .path(path)
                            .methodName(methodName)
                            .fieldName(field.getSimpleName().toString())
                            .targetEntityModel(JpaEntityModelFactory.get(elementType, env))
                            .mappedBy(mappedBy)
                            .build());
                }
            }
        }

        return result;
    }


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
        var elementType = getElementType(getter.getReturnType(), getter, env);

        return ReadEndpointRelation.builder()
                .path(path)
                .methodName(getter.getSimpleName().toString())
                .targetEntityModel(JpaEntityModelFactory.get(elementType, env))
                .build();
    }

    private static TypeElement getElementType(TypeMirror returnType, Element element, ProcessingEnvironment env) {

        if (!(returnType instanceof DeclaredType declaredReturnType)) {
            LoggingUtils.warn(env, element, "Generating endpoints can only be used on declared return types");
            return null;
        }

        var typeArgs = declaredReturnType.getTypeArguments();
        if (typeArgs.size() != 1) {
            LoggingUtils.warn(env, element, "Generating endpoints must return a generic collection");
            return null;
        }

        var elementTypeMirror = typeArgs.getFirst();

        if (!(elementTypeMirror instanceof DeclaredType elementDeclaredType)) {
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
