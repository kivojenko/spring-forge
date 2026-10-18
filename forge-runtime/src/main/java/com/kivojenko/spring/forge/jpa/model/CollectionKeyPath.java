package com.kivojenko.spring.forge.jpa.model;

import com.kivojenko.spring.forge.jpa.factory.JpaEntityModelFactory;
import com.kivojenko.spring.forge.jpa.model.base.JpaEntityModel;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.TreeSet;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

/**
 * A get-or-create key path that goes through a collection, such as {@code synonyms} (an {@code @ElementCollection}
 * of basic values) or {@code numberReferences.casNumbers.number} (the {@code @Id} of the elements of a
 * {@code @ManyToMany} collection inside an {@code @Embedded} object).
 * <p>
 * An entity matches the key when any element of the collection has the given value; a created entity gets the
 * value added to the collection.
 *
 * @param holders         {@code @Embedded} fields leading to the collection, outermost first (empty when the
 *                        collection is declared on the entity itself)
 * @param holderTypes     types of the {@code holders}, in the same order
 * @param collectionField name of the collection field
 * @param collectionType  declared type of the collection field, e.g. {@code Set<Cas>}
 * @param elementModel    model of the element entity, or {@code null} for a collection of basic values
 * @param leafType        type of the key value: the element type for basic values, the element's ID type otherwise
 */
public record CollectionKeyPath(
    List<String> holders,
    List<TypeElement> holderTypes,
    String collectionField,
    DeclaredType collectionType,
    JpaEntityModel elementModel,
    TypeName leafType
)
{
  /** @return whether the elements are entities linked by their ID (otherwise they are basic values) */
  public boolean hasEntityElements() {
    return elementModel != null;
  }

  /**
   * Resolves a key path of the given entity that goes through a collection.
   *
   * @param env    the processing environment
   * @param entity the entity declaring the key
   * @param path   the key path, e.g. {@code numberReferences.casNumbers.number}
   * @return the resolved path, or {@code null} when the path does not go through a collection
   * @throws IllegalStateException when the path goes through a collection in a way get-or-create cannot support
   */
  public static CollectionKeyPath resolve(ProcessingEnvironment env, TypeElement entity, String path) {
    var types = env.getTypeUtils();
    var elements = env.getElementUtils();
    var collectionErasure = types.erasure(elements.getTypeElement("java.util.Collection").asType());
    var mapErasure = types.erasure(elements.getTypeElement("java.util.Map").asType());

    var parts = path.split("\\.");
    var holders = new ArrayList<String>();
    var holderTypes = new ArrayList<TypeElement>();
    TypeElement current = entity;

    for (int i = 0; i < parts.length; i++) {
      var segment = parts[i];
      var field = findField(current, segment);
      if (field == null) return null; // reported by the regular key resolution
      var type = field.asType();

      if (type.getKind() == TypeKind.DECLARED && types.isAssignable(types.erasure(type), mapErasure)) {
        throw error(entity, path, "'" + segment + "' is a Map; maps are not supported");
      }
      if (type.getKind() != TypeKind.DECLARED || !types.isAssignable(types.erasure(type), collectionErasure)) {
        if (i == parts.length - 1) return null; // a single-valued path
        if (type.getKind() != TypeKind.DECLARED) {
          throw error(entity, path, "'" + segment + "' is not an object and cannot be traversed");
        }
        var next = (TypeElement) ((DeclaredType) type).asElement();
        if (next.getAnnotation(Entity.class) != null) {
          // A single-valued relation path (country.code) is handled by the regular key resolution
          rejectCollectionAhead(env, next, parts, i + 1, entity, path);
          return null;
        }
        holders.add(segment);
        holderTypes.add(next);
        current = next;
        continue;
      }

      // The collection segment
      if (isInverseSide(field)) {
        throw error(entity, path, "'" + segment + "' is the inverse (mappedBy) side of its association; "
            + "adding to it would not be persisted");
      }
      var declared = (DeclaredType) type;
      if (declared.getTypeArguments().size() != 1 || declared.getTypeArguments().getFirst().getKind() != TypeKind.DECLARED) {
        throw error(entity, path, "the element type of '" + segment + "' cannot be determined");
      }
      var elementType = declared.getTypeArguments().getFirst();
      var element = (TypeElement) ((DeclaredType) elementType).asElement();
      var rest = parts.length - 1 - i;

      if (element.getAnnotation(Entity.class) != null) {
        var elementModel = JpaEntityModelFactory.get(element);
        var idName = elementModel.getJpaId().name();
        if (rest != 1 || !parts[i + 1].equals(idName)) {
          throw error(entity, path, "a collection of entities must be keyed by the element's ID: expected '"
              + String.join(".", Arrays.copyOfRange(parts, 0, i + 1)) + "." + idName + "'");
        }
        return new CollectionKeyPath(holders, holderTypes, segment, declared, elementModel, elementModel.getJpaId().type());
      }
      if (element.getAnnotation(Embeddable.class) != null) {
        throw error(entity, path, "collections of @Embeddable values are not supported");
      }
      if (rest != 0) {
        throw error(entity, path, "'" + segment + "' holds basic values; the path must end at the collection");
      }
      return new CollectionKeyPath(holders, holderTypes, segment, declared, null, TypeName.get(elementType));
    }
    return null;
  }

  /**
   * Picks the class used to initialize the collection when it is {@code null}.
   *
   * @param env the processing environment
   * @return the declared class when it is concrete, otherwise a {@code java.util} implementation of it
   */
  public TypeName collectionImplementation(ProcessingEnvironment env) {
    var raw = (TypeElement) collectionType.asElement();
    if (raw.getKind() == ElementKind.CLASS && !raw.getModifiers().contains(Modifier.ABSTRACT)) {
      return ClassName.get(raw);
    }
    var types = env.getTypeUtils();
    var elements = env.getElementUtils();
    var erasure = types.erasure(collectionType);
    if (types.isAssignable(erasure, types.erasure(elements.getTypeElement("java.util.SortedSet").asType()))) {
      return ClassName.get(TreeSet.class);
    }
    if (types.isAssignable(erasure, types.erasure(elements.getTypeElement("java.util.Set").asType()))) {
      return ClassName.get(LinkedHashSet.class);
    }
    if (types.isAssignable(types.erasure(elements.getTypeElement("java.util.ArrayList").asType()), erasure)) {
      return ClassName.get(ArrayList.class);
    }
    throw new IllegalStateException("Cannot instantiate collection type " + collectionType + " of '" + collectionField + "'");
  }

  private static void rejectCollectionAhead(
      ProcessingEnvironment env, TypeElement relation, String[] parts, int from, TypeElement entity, String path
  )
  {
    var types = env.getTypeUtils();
    var collectionErasure = types.erasure(env.getElementUtils().getTypeElement("java.util.Collection").asType());
    TypeElement current = relation;
    for (int i = from; i < parts.length; i++) {
      var field = findField(current, parts[i]);
      if (field == null) return;
      var type = field.asType();
      if (type.getKind() != TypeKind.DECLARED) return;
      if (types.isAssignable(types.erasure(type), collectionErasure)) {
        throw error(entity, path, "the collection '" + parts[i] + "' is reached through a related entity; "
            + "only @Embedded objects may lead to a collection key");
      }
      current = (TypeElement) ((DeclaredType) type).asElement();
    }
  }

  private static boolean isInverseSide(VariableElement field) {
    var oneToMany = field.getAnnotation(OneToMany.class);
    if (oneToMany != null && !oneToMany.mappedBy().isEmpty()) return true;
    var manyToMany = field.getAnnotation(ManyToMany.class);
    return manyToMany != null && !manyToMany.mappedBy().isEmpty();
  }

  private static VariableElement findField(TypeElement type, String name) {
    TypeElement current = type;
    while (current != null) {
      for (var enclosed : current.getEnclosedElements()) {
        if (enclosed.getKind() == ElementKind.FIELD && enclosed.getSimpleName().contentEquals(name)) {
          return (VariableElement) enclosed;
        }
      }
      TypeMirror superType = current.getSuperclass();
      current = superType.getKind() == TypeKind.DECLARED ? (TypeElement) ((DeclaredType) superType).asElement() : null;
    }
    return null;
  }

  private static IllegalStateException error(TypeElement entity, String path, String reason) {
    return new IllegalStateException("Unsupported @GetOrCreate key '" + path + "' on " + entity.getSimpleName() + ": " + reason);
  }
}
