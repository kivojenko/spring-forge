package com.kivojenko.spring.forge.jpa.contract;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * Abstract base implementation of controller without POST create endpoints that delegates to a {@link ForgeService}.
 * Used when the entity is abstract or creation via POST is not supported.
 *
 * @param <E>  the entity type
 * @param <ID> the ID type
 * @param <R>  the repository type
 * @param <S>  the service type
 */
@RestController
public abstract class ForgeAbstractController<E, ID, R extends JpaRepository<E, ID>, S extends ForgeService<E, ID, R>> {

  @Autowired
  protected S service;

  /**
   * Delegates to {@link ForgeService#count()}.
   *
   * @return total number of entities
   */
  @GetMapping("/count")
  public long count() {
    return service.count();
  }

  /**
   * Delegates to {@link ForgeService#getById(Object)}.
   *
   * @param id the ID of the entity to retrieve
   * @return the found entity
   */
  @GetMapping("/{id}")
  public E getById(@PathVariable(name = "id") ID id) {
    return service.getById(id);
  }

  /**
   * Delegates to {@link ForgeService#exists(Object)}.
   *
   * @param id the ID of the entity to check
   * @return true if the entity exists, false otherwise
   */
  @RequestMapping(method = RequestMethod.HEAD, path = "/{id}")
  public boolean exists(@PathVariable(name = "id") ID id) {
    return service.exists(id);
  }

  /**
   * Delegates to {@link ForgeService#update(Object, Object)}.
   *
   * @param id the ID of the entity to update
   * @param entity the entity with updated fields
   * @return the updated entity
   */
  @PutMapping("/{id}")
  @ResponseStatus(code = HttpStatus.CREATED)
  public E update(@PathVariable(name = "id") ID id, @Valid @RequestBody E entity) {
    return service.update(id, entity);
  }

  /**
   * Delegates to {@link ForgeService#update(Object, Object)} using the HTTP PATCH method.
   * Semantically this behaves as an update operation; partial update semantics are
   * determined by the service implementation.
   *
   * @param id the ID of the entity to update
   * @param fields the fields to update
   * @return the updated entity
   */
  @PatchMapping("/{id}")
  public E patch(@PathVariable(name = "id") ID id, @RequestBody java.util.Map<String, Object> fields) {
    return service.patch(id, fields);
  }

  /**
   * Delegates to {@link ForgeService#deleteById(Object)}.
   *
   * @param id the ID of the entity to delete
   */
  @DeleteMapping("/{id}")
  @ResponseStatus(code = HttpStatus.NO_CONTENT)
  public void delete(@PathVariable(name = "id") ID id) {
    service.deleteById(id);
  }

  private static final java.util.Map<String, java.lang.reflect.Member> ACCESSOR_CACHE =
      new java.util.concurrent.ConcurrentHashMap<>();
  private static final java.lang.reflect.Member NOT_FOUND = ForgeAbstractController.class.getDeclaredMethods()[0];

  /**
   * Sorts the given iterable of items according to the provided {@link org.springframework.data.domain.Sort} specification.
   *
   * @param items the items to sort
   * @param sort the sort criteria
   * @param <T> the item type
   * @return the sorted iterable (as a List)
   */
  @SuppressWarnings("unchecked")
  protected <T> Iterable<T> sort(Iterable<T> items, org.springframework.data.domain.Sort sort) {
    if (items == null || sort == null || sort.isUnsorted()) {
      return items;
    }
    java.util.List<T> list = new java.util.ArrayList<>();
    items.forEach(list::add);
    if (list.size() <= 1) {
      return list;
    }
    list.sort((a, b) -> {
      if (a == b) return 0;
      if (a == null) return -1;
      if (b == null) return 1;

      for (org.springframework.data.domain.Sort.Order order : sort) {
        String prop = order.getProperty();
        boolean isDesc = order.isDescending() || "desc".equalsIgnoreCase(prop);
        int cmp = compareProperty(a, b, prop, order.isIgnoreCase());
        if (cmp != 0) {
          return isDesc ? -cmp : cmp;
        }
      }
      return 0;
    });
    return list;
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private static int compareProperty(Object a, Object b, String property, boolean ignoreCase) {
    boolean propertyIsDirection = "desc".equalsIgnoreCase(property) || "asc".equalsIgnoreCase(property);
    if (property == null || property.isBlank() || propertyIsDirection) {
      if (a instanceof Comparable cA && b instanceof Comparable cB) {
        if (ignoreCase && cA instanceof String sA && cB instanceof String sB) {
          return sA.compareToIgnoreCase(sB);
        }
        try {
          return ((Comparable<Object>) cA).compareTo(cB);
        } catch (Exception ignored) {
          return cA.toString().compareTo(cB.toString());
        }
      } else if (a != null && b != null) {
        return a.toString().compareTo(b.toString());
      }
      return 0;
    }

    Object valA = extractProperty(a, property);
    Object valB = extractProperty(b, property);

    if (valA == valB) return 0;
    if (valA == null) return -1;
    if (valB == null) return 1;

    if (ignoreCase && valA instanceof String sA && valB instanceof String sB) {
      return sA.compareToIgnoreCase(sB);
    }
    if (valA instanceof Comparable cA && valB instanceof Comparable cB) {
      try {
        return ((Comparable<Object>) cA).compareTo(cB);
      } catch (Exception ignored) {
      }
    }
    return valA.toString().compareTo(valB.toString());
  }

  private static Object extractProperty(Object obj, String propertyPath) {
    if (obj == null || propertyPath == null || propertyPath.isBlank()) {
      return obj;
    }
    if (propertyPath.contains(".")) {
      String[] parts = propertyPath.split("\\.", 2);
      Object next = extractSingleProperty(obj, parts[0]);
      return extractProperty(next, parts[1]);
    }
    return extractSingleProperty(obj, propertyPath);
  }

  private static Object extractSingleProperty(Object obj, String propName) {
    if (obj == null) return null;
    if (obj instanceof java.util.Map<?, ?> map) {
      return map.get(propName);
    }
    Class<?> clazz = obj.getClass();
    String cacheKey = clazz.getName() + "#" + propName;
    java.lang.reflect.Member member = ACCESSOR_CACHE.computeIfAbsent(cacheKey, k -> findAccessor(clazz, propName));
    if (member == null || member == NOT_FOUND) {
      return obj instanceof Comparable ? obj : null;
    }
    try {
      if (member instanceof java.lang.reflect.Method method) {
        return method.invoke(obj);
      } else if (member instanceof java.lang.reflect.Field field) {
        return field.get(obj);
      }
    } catch (Exception ignored) {
    }
    return null;
  }

  private static java.lang.reflect.Member findAccessor(Class<?> clazz, String propName) {
    String capitalized = propName.isEmpty() ? "" : Character.toUpperCase(propName.charAt(0)) + propName.substring(1);
    String[] methodCandidates = new String[] {
        "get" + capitalized,
        "is" + capitalized,
        propName
    };
    for (String methodName : methodCandidates) {
      try {
        java.lang.reflect.Method method = clazz.getMethod(methodName);
        method.setAccessible(true);
        return method;
      } catch (NoSuchMethodException ignored) {
      }
    }
    Class<?> current = clazz;
    while (current != null && current != Object.class) {
      try {
        java.lang.reflect.Field field = current.getDeclaredField(propName);
        field.setAccessible(true);
        return field;
      } catch (NoSuchFieldException ignored) {
      }
      current = current.getSuperclass();
    }
    return NOT_FOUND;
  }
}
