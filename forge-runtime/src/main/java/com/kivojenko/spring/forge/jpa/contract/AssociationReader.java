package com.kivojenko.spring.forge.jpa.contract;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.EntityManager;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.EmbeddableType;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.Metamodel;
import jakarta.persistence.metamodel.PluralAttribute;
import jakarta.persistence.metamodel.SingularAttribute;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import org.hibernate.Hibernate;
import org.springframework.core.annotation.AnnotatedElementUtils;

/**
 * Reads the entities of a collection association together with everything a JSON view will write for them, in a
 * number of queries that depends on the shape of the entity graph and not on the number of rows.
 *
 * <p>Starting from the associated entities, it walks the JPA metamodel level by level. Of each entity it loads, in
 * one query for all the single-valued associations and one per collection (joining several sibling collections in one
 * query would multiply the rows), every association that
 * <ul>
 *   <li>the view can write: not {@code @JsonIgnore}d, not write-only, and not limited by {@code @JsonView} to views
 *   the given one is not part of (meta-annotations such as a custom {@code @FullView} count); a property with no
 *   {@code @JsonView} is taken to be written, which can only over-fetch; or</li>
 *   <li>is {@code EAGER}, which Hibernate would otherwise load one row at a time.</li>
 * </ul>
 * The entities found that way are the next level; one already seen is not loaded again, so a self-referencing
 * association such as {@code parent} is followed until it ends.
 *
 * <p>Properties computed in a getter rather than backed by an association are not seen: whatever such a getter
 * reads has to be a visible property of its own.
 */
public final class AssociationReader {

  private final EntityManager em;
  private final Metamodel metamodel;
  private final Class<?> view;
  private final Set<Class<?>> entityClasses;
  private final Set<String> seen = new HashSet<>();

  /**
   * @param em   the entity manager of the current transaction
   * @param view the Jackson view the result is going to be written with
   */
  public AssociationReader(EntityManager em, Class<?> view) {
    this.em = em;
    this.view = view;
    this.metamodel = em.getMetamodel();
    this.entityClasses = metamodel.getEntities().stream().map(EntityType::getJavaType).collect(Collectors.toSet());
  }

  /**
   * Loads {@code attribute} of {@code owner}, and what the view writes of it.
   *
   * @param ownerClass the entity class of the owner
   * @param owner      the owner, already loaded
   * @param attribute  the name of a collection attribute of the owner
   * @return the associated entities, in no particular order
   */
  @SuppressWarnings("unchecked")
  public <T> List<T> read(Class<?> ownerClass, Object owner, String attribute) {
    var ownerType = metamodel.entity(ownerClass);
    if (!(ownerType.getAttribute(attribute) instanceof PluralAttribute<?, ?, ?> plural)) {
      throw new IllegalArgumentException(ownerClass.getSimpleName() + "." + attribute + " is not a collection");
    }

    var target = plural.getElementType().getJavaType();
    var hql = new StringBuilder("select distinct t from " + ownerType.getName() + " o join o." + attribute + " t");
    eagerJoins(hql, "t", "", metamodel.entity(target).getAttributes(), Set.of(target), new AtomicInteger());
    var rows = (List<T>) em
        .createQuery(hql.append(" where o = :owner").toString(), target)
        .setParameter("owner", owner)
        .getResultList();

    var level = discover(rows);
    while (!level.isEmpty()) {
      var next = new LinkedHashMap<Class<?>, List<Object>>();
      level.forEach((type, entities) -> load(type, entities).forEach(
          (nextType, found) -> next.computeIfAbsent(nextType, k -> new ArrayList<>()).addAll(found)
      ));
      level = next;
    }
    return rows;
  }

  /** Loads the associations of {@code entities}, and returns the entities they lead to that are not yet known. */
  private Map<Class<?>, List<Object>> load(Class<?> type, List<Object> entities) {
    var entityType = metamodel.entity(type);
    var singles = new ArrayList<Step>();
    var collections = new ArrayList<Step>();
    collect(entityType.getAttributes(), "", singles, collections);

    if (!singles.isEmpty()) {
      var joins = new StringBuilder();
      var counter = new AtomicInteger();
      singles.forEach(step -> join(joins, "x", step.path(), step.attribute(), Set.of(type), counter));
      run(entityType, joins.toString(), entities);
    }
    for (var step : collections) {
      var joins = new StringBuilder();
      join(joins, "x", step.path(), step.attribute(), Set.of(type), new AtomicInteger());
      run(entityType, joins.toString(), entities);
    }

    var found = new ArrayList<Object>();
    for (var step : concat(singles, collections)) {
      for (var entity : entities) {
        values(entity, step.path()).forEach(found::add);
      }
    }
    return discover(found);
  }

  private void run(EntityType<?> type, String joins, List<Object> entities) {
    em.createQuery("select distinct x from " + type.getName() + " x" + joins + " where x in :entities")
        .setParameter("entities", entities)
        .getResultList();
  }

  private void collect(
      Set<? extends Attribute<?, ?>> attributes,
      String prefix,
      List<Step> singles,
      List<Step> collections
  ) {
    for (var attribute : attributes) {
      var path = prefix + attribute.getName();
      switch (attribute.getPersistentAttributeType()) {
        case MANY_TO_ONE, ONE_TO_ONE -> {
          if (needed(attribute)) singles.add(new Step(path, attribute));
        }
        case ONE_TO_MANY, MANY_TO_MANY, ELEMENT_COLLECTION -> {
          if (needed(attribute)) collections.add(new Step(path, attribute));
        }
        case EMBEDDED -> {
          if (visible(attribute) && attribute instanceof SingularAttribute<?, ?> single
              && single.getType() instanceof EmbeddableType<?> embeddable) {
            collect(embeddable.getAttributes(), path + ".", singles, collections);
          }
        }
        default -> {
        }
      }
    }
  }

  /** An association to load, by its dotted path from the entity (through embedded objects). */
  private record Step(String path, Attribute<?, ?> attribute) {
  }

  /**
   * {@code left join fetch owner.path alias}, followed by the same for the {@code EAGER} associations of what it
   * leads to. The latter is not optional: Hibernate resolves the eager associations of an entity the moment it
   * materialises it, one select per entity, so unless they come in the same query a "fixed" plan still grows by a
   * select per row. {@code chain} holds the entity classes already on the way down, which ends a cycle.
   */
  private void join(
      StringBuilder hql,
      String owner,
      String path,
      Attribute<?, ?> attribute,
      Set<Class<?>> chain,
      AtomicInteger counter
  ) {
    var alias = "j" + counter.incrementAndGet();
    hql.append(" left join fetch ").append(owner).append('.').append(path).append(' ').append(alias);

    var target = attribute instanceof PluralAttribute<?, ?, ?> plural
        ? plural.getElementType().getJavaType()
        : attribute.getJavaType();
    if (entityClasses.contains(target) && !chain.contains(target)) {
      var next = new HashSet<>(chain);
      next.add(target);
      eagerJoins(hql, alias, "", metamodel.entity(target).getAttributes(), next, counter);
    }
  }

  private void eagerJoins(
      StringBuilder hql,
      String alias,
      String prefix,
      Set<? extends Attribute<?, ?>> attributes,
      Set<Class<?>> chain,
      AtomicInteger counter
  ) {
    for (var attribute : attributes) {
      var path = prefix + attribute.getName();
      switch (attribute.getPersistentAttributeType()) {
        case MANY_TO_ONE, ONE_TO_ONE, ONE_TO_MANY, MANY_TO_MANY, ELEMENT_COLLECTION -> {
          if (eager(attribute)) join(hql, alias, path, attribute, chain, counter);
        }
        case EMBEDDED -> {
          if (attribute instanceof SingularAttribute<?, ?> single
              && single.getType() instanceof EmbeddableType<?> embeddable) {
            eagerJoins(hql, alias, path + ".", embeddable.getAttributes(), chain, counter);
          }
        }
        default -> {
        }
      }
    }
  }

  /** Entities among {@code values} that are not yet known, by their class: a subclass instance is not a proxy. */
  private Map<Class<?>, List<Object>> discover(Collection<?> values) {
    var found = new LinkedHashMap<Class<?>, List<Object>>();
    for (var value : values) {
      if (value == null) continue;
      var entity = Hibernate.unproxy(value);
      if (!entityClasses.contains(entity.getClass())) continue;
      var key = entity.getClass().getName() + "#" + em.getEntityManagerFactory().getPersistenceUnitUtil().getIdentifier(entity);
      if (seen.add(key)) {
        found.computeIfAbsent(entity.getClass(), k -> new ArrayList<>()).add(entity);
      }
    }
    return found;
  }

  /** The value at {@code path} (dotted through embedded objects), flattened: a collection becomes its elements. */
  private static List<Object> values(Object entity, String path) {
    Object current = entity;
    for (var name : path.split("\\.")) {
      if (current == null) return List.of();
      current = read(Hibernate.unproxy(current), name);
    }
    if (current instanceof Map<?, ?> map) return new ArrayList<>(map.values());
    if (current instanceof Collection<?> collection) return new ArrayList<>(collection);
    return current == null ? List.of() : List.of(current);
  }

  private static Object read(Object target, String name) {
    try {
      var field = findField(target.getClass(), name);
      if (field != null) {
        field.setAccessible(true);
        return field.get(target);
      }
      var getter = findGetter(target.getClass(), name);
      if (getter != null) {
        getter.setAccessible(true);
        return getter.invoke(target);
      }
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException("Cannot read " + target.getClass().getName() + "." + name, e);
    }
    return null;
  }

  private boolean needed(Attribute<?, ?> attribute) {
    return eager(attribute) || visible(attribute);
  }

  /** {@code EAGER} as the JPA defaults have it: to-one associations unless said otherwise, collections only if said. */
  private static boolean eager(Attribute<?, ?> attribute) {
    if (!(attribute.getJavaMember() instanceof AnnotatedElement element)) return false;
    return switch (attribute.getPersistentAttributeType()) {
      case MANY_TO_ONE -> {
        var annotation = element.getAnnotation(ManyToOne.class);
        yield annotation == null || annotation.fetch() == FetchType.EAGER;
      }
      case ONE_TO_ONE -> {
        var annotation = element.getAnnotation(OneToOne.class);
        yield annotation == null || annotation.fetch() == FetchType.EAGER;
      }
      case ONE_TO_MANY -> {
        var annotation = element.getAnnotation(OneToMany.class);
        yield annotation != null && annotation.fetch() == FetchType.EAGER;
      }
      case MANY_TO_MANY -> {
        var annotation = element.getAnnotation(ManyToMany.class);
        yield annotation != null && annotation.fetch() == FetchType.EAGER;
      }
      case ELEMENT_COLLECTION -> {
        var annotation = element.getAnnotation(ElementCollection.class);
        yield annotation != null && annotation.fetch() == FetchType.EAGER;
      }
      default -> false;
    };
  }

  /** Whether Jackson, writing {@code view}, would write this attribute. Unsure means yes. */
  private boolean visible(Attribute<?, ?> attribute) {
    var declaring = attribute.getDeclaringType().getJavaType();
    var elements = new ArrayList<AnnotatedElement>(2);
    var getter = findGetter(declaring, attribute.getName());
    var field = findField(declaring, attribute.getName());
    if (getter != null) elements.add(getter);
    if (field != null) elements.add(field);

    for (var element : elements) {
      var ignore = AnnotatedElementUtils.findMergedAnnotation(element, JsonIgnore.class);
      if (ignore != null && ignore.value()) return false;
      var property = AnnotatedElementUtils.findMergedAnnotation(element, JsonProperty.class);
      if (property != null && property.access() == JsonProperty.Access.WRITE_ONLY) return false;
    }
    for (var element : elements) {
      var jsonView = AnnotatedElementUtils.findMergedAnnotation(element, JsonView.class);
      if (jsonView != null) {
        return Arrays.stream(jsonView.value()).anyMatch(allowed -> allowed.isAssignableFrom(view));
      }
    }
    return true;
  }

  private static java.lang.reflect.Field findField(Class<?> type, String name) {
    for (var current = type; current != null && current != Object.class; current = current.getSuperclass()) {
      try {
        return current.getDeclaredField(name);
      } catch (NoSuchFieldException ignored) {
      }
    }
    return null;
  }

  private static Method findGetter(Class<?> type, String name) {
    var suffix = Character.toUpperCase(name.charAt(0)) + name.substring(1);
    for (var candidate : new String[] {"get" + suffix, "is" + suffix}) {
      for (var current = type; current != null && current != Object.class; current = current.getSuperclass()) {
        try {
          return current.getDeclaredMethod(candidate);
        } catch (NoSuchMethodException ignored) {
        }
      }
    }
    return null;
  }

  private static List<Step> concat(List<Step> first, List<Step> second) {
    var all = new ArrayList<>(first);
    all.addAll(second);
    return all;
  }
}
