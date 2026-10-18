package com.kivojenko.spring.forge.jpa.model;

import com.kivojenko.spring.forge.annotation.filter.FilterSearchField;
import com.kivojenko.spring.forge.annotation.filter.StringMatchMode;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import lombok.Builder;
import lombok.Data;

import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;

import java.util.ArrayList;
import java.util.List;

import static com.kivojenko.spring.forge.jpa.utils.ClassNameUtils.EXPRESSIONS;
import static com.kivojenko.spring.forge.jpa.utils.ClassNameUtils.JPA_EXPRESSIONS;

@Data
@Builder
public class FilterSearchFieldModel {
  VariableElement element;
  TypeElement typeElement;
  FilterSearchField annotation;
  String name;
  StringMatchMode stringMatchMode;
  String targetField;
  String targetPath;
  String targetFieldName;
  boolean originalIterable;
  boolean originalSingleEntity;
  boolean originalEmbedded;
  boolean scalarCollection;
  /**
   * The path type each collection on the way to the searched value is joined as, in path order: a
   * {@code Q} class, or {@code null} for a collection of scalars. Empty when no collection is involved.
   */
  @Builder.Default
  List<ClassName> joinAliasTypes = List.of();

  public String getName() {
    if (name != null && !name.isEmpty()) {
      return name;
    }
    if (annotation != null && !annotation.name().isEmpty()) {
      return annotation.name();
    }
    return "search";
  }

  public String getTargetFieldName() {
    if (targetFieldName != null) {
      return targetFieldName;
    }
    String fieldName = element != null ? element.getSimpleName().toString() : "";
    if (scalarCollection && (targetField == null || targetField.isEmpty())) {
      return fieldName + ".any()";
    }
    return resolvedPath(fieldName);
  }

  private String resolvedPath(String fieldName) {
    if (targetField == null || targetField.isEmpty()) {
      return fieldName;
    }
    var target = targetPath == null || targetPath.isEmpty() ? targetField : targetPath;
    if (originalIterable) {
      return fieldName + ".any()." + target;
    }
    if (originalSingleEntity || originalEmbedded) {
      return fieldName + "." + target;
    }
    return target;
  }

  /** Whether the searched value sits behind a collection, so it is matched with an id subquery. */
  public boolean isCollectionSearch() {
    return joinAliasTypes != null && !joinAliasTypes.isEmpty() && targetFieldName == null;
  }

  /**
   * ORs this field's match for {@code searchParam} into {@code sink}.
   *
   * <p>A plain path is matched directly. A path through collections is matched as
   * {@code entity.id in (select root.id from Entity root join root.items i … where i.value op :search)}:
   * the subquery does not depend on the outer row, so the database evaluates it once and probes each
   * row against the matching ids, instead of running a correlated {@code exists} per row as
   * {@code .any()} would.
   *
   * @param index          distinguishes this field's subquery aliases from the other fields of the group
   * @param entityPathType the {@code Q} class of the searched entity
   * @param idName         the entity's id property
   */
  public void addSearchPredicate(
      MethodSpec.Builder builder,
      String sink,
      String searchParam,
      int index,
      String op,
      TypeName entityPathType,
      String idName
  ) {
    var segments = collectionSegments();
    if (segments == null) {
      builder.addStatement("$L.or(entity.$L.$L($L))", sink, getTargetFieldName(), op, searchParam);
      return;
    }

    var prefix = searchParam + index;
    var root = "__" + prefix + "Root";
    builder.addStatement("var $L = new $T($S)", root, entityPathType, prefix + "Root");

    var subquery = CodeBlock.builder()
        .add("$T.select($L.$L).from($L)", JPA_EXPRESSIONS, root, idName, root);
    var previous = root;
    for (int i = 0; i < joinAliasTypes.size(); i++) {
      var join = "__" + prefix + "Join" + i;
      var alias = prefix + "Join" + i;
      var aliasType = joinAliasTypes.get(i);
      if (aliasType == null) {
        builder.addStatement("var $L = $T.stringPath($S)", join, EXPRESSIONS, alias);
      } else {
        builder.addStatement("var $L = new $T($S)", join, aliasType, alias);
      }
      subquery.add(".innerJoin($L.$L, $L)", previous, segments.get(i), join);
      previous = join;
    }

    var leaf = segments.getLast();
    var value = leaf.isEmpty() ? previous : previous + "." + leaf;
    subquery.add(".where($L.$L($L))", value, op, searchParam);

    builder.addStatement("$L.or(entity.$L.in($L))", sink, idName, subquery.build());
  }

  /**
   * Splits the target path at every collection: one join path per collection, followed by the path from
   * the last joined element to the searched value (empty when the element itself is searched).
   *
   * @return the segments, or {@code null} when the value is reached without crossing a collection
   */
  private List<String> collectionSegments() {
    if (!isCollectionSearch()) {
      return null;
    }
    var path = getTargetFieldName();
    if (!path.endsWith(".any()") && scalarCollection) {
      path += ".any()";
    }
    var parts = path.split("\\.any\\(\\)", -1);
    if (parts.length != joinAliasTypes.size() + 1) {
      return null;
    }
    var segments = new ArrayList<String>();
    for (var part : parts) {
      segments.add(part.startsWith(".") ? part.substring(1) : part);
    }
    return segments;
  }
}
