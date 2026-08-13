package com.kivojenko.spring.forge.jpa.contract;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.util.ReflectionUtils;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

/**
 * Abstract base class for generated services.
 * Provides standard business logic for CRUD operations.
 *
 * @param <E>  the entity type
 * @param <ID> the ID type
 * @param <R>  the repository type
 */
public abstract class ForgeService<E, ID, R extends JpaRepository<E, ID>> {
  @Autowired
  protected R repository;

  @PersistenceContext
  protected EntityManager entityManager;

  /**
   * Fixes or transforms the given entity before it is saved.
   * By default, returns the entity as is.
   *
   * @param entity the entity to fix
   * @return the fixed entity
   */
  public E fixParameters(E entity) {
    return entity;
  }

  /**
   * Retrieves an entity by its ID.
   *
   * @param id the ID of the entity to retrieve
   * @return the found entity
   * @throws EntityNotFoundException if no entity with the given ID exists
   */
  public E getById(ID id) {
    return repository.findById(id).orElseThrow(EntityNotFoundException::new);
  }

  /**
   * Checks if an entity with the given ID exists.
   *
   * @param id the ID to check
   * @return true if it exists, false otherwise
   */
  public boolean exists(ID id) {
    return repository.existsById(id);
  }

  /**
   * Creates a new entity.
   *
   * @param entity the entity to create
   * @return the saved entity
   */
  @Transactional
  public E create(E entity) {
    entity = fixParameters(entity);

    // Ensure DB-generated values (e.g., defaults, triggers) are materialized
    entity = repository.saveAndFlush(entity);
    entityManager.refresh(entity);
    return entity;
  }

  /**
   * Updates an existing entity.
   *
   * @param id     the ID of the entity to update
   * @param entity the entity with updated data
   * @return the saved entity
   * @throws EntityNotFoundException if the entity with the given ID does not exist
   */
  @Transactional
  public E update(ID id, E entity) {
    if (!repository.existsById(id)) {
      throw new EntityNotFoundException();
    }
    setId(entity, id);
    entity = fixParameters(entity);

    return repository.save(entity);
  }

  /**
   * Applies a partial update to an existing entity. Fields present in the {@code fields} map
   * are applied to the persisted entity by name. The {@code id} field (if present) is ignored.
   *
   * <p>Notes:
   * <ul>
   *   <li>Only top-level scalar fields are supported by default; nested objects/collections
   *   require a custom service override.</li>
   *   <li>Type conversion is attempted for common cases (numbers, booleans, enums, strings).</li>
   * </ul>
   *
   * @param id the ID of the entity to patch
   * @param fields map of field names to desired values
   * @return the updated entity
   */
  @Transactional
  public E patch(ID id, Map<String, Object> fields) {
    var entity = repository.findById(id).orElseThrow(EntityNotFoundException::new);

    if (fields == null || fields.isEmpty()) {
      return entity;
    }

    Class<?> clazz = entity.getClass();
    for (var entry : fields.entrySet()) {
      String name = entry.getKey();
      if (name == null || name.isBlank() || name.equalsIgnoreCase("id")) {
        continue;
      }

      Field field = ReflectionUtils.findField(clazz, name);
      if (field == null) {
        continue; // unknown field — skip
      }
      field.setAccessible(true);

      Object value = convertValue(entry.getValue(), field.getType());
      ReflectionUtils.setField(field, entity, value);
    }

    entity = fixParameters(entity);
    return repository.save(entity);
  }

  private Object convertValue(Object raw, Class<?> targetType) {
    if (raw == null) return null;

    if (targetType.isInstance(raw)) return raw;

    // Strings
    if (targetType == String.class) return String.valueOf(raw);

    // Booleans
    if (targetType == boolean.class || targetType == Boolean.class) {
      if (raw instanceof Boolean b) return b;
      return Boolean.parseBoolean(String.valueOf(raw));
    }

    // Numbers
    if (Number.class.isAssignableFrom(targetType) || targetType.isPrimitive()) {
      Number n = (raw instanceof Number) ? (Number) raw : parseNumber(String.valueOf(raw));
      if (n == null) return null;
      if (targetType == byte.class || targetType == Byte.class) return n.byteValue();
      if (targetType == short.class || targetType == Short.class) return n.shortValue();
      if (targetType == int.class || targetType == Integer.class) return n.intValue();
      if (targetType == long.class || targetType == Long.class) return n.longValue();
      if (targetType == float.class || targetType == Float.class) return n.floatValue();
      if (targetType == double.class || targetType == Double.class) return n.doubleValue();
    }

    // Enums
    if (targetType.isEnum()) {
      String name = String.valueOf(raw);
      @SuppressWarnings({"unchecked", "rawtypes"})
      Object enumVal = Enum.valueOf((Class<Enum>) targetType, name);
      return enumVal;
    }

    // Fallback — best effort string conversion
    return targetType.cast(raw);
  }

  private Number parseNumber(String s) {
    try {
      if (s.contains(".")) {
        return Double.parseDouble(s);
      }
      return Long.parseLong(s);
    } catch (NumberFormatException ex) {
      return null;
    }
  }

  /**
   * Sets the ID on the given entity.
   * Must be implemented by generated services to handle different ID types and field names.
   *
   * @param entity the entity to set the ID on
   * @param id     the ID value
   */
  protected abstract void setId(E entity, ID id);

  /**
   * Returns the class of the entity handled by this service.
   *
   * @return the entity class
   */
  public abstract Class<E> getEntityClass();


  /**
   * Counts the total number of entities.
   *
   * @return the total count
   */
  public long count() {
    return repository.count();
  }

  /**
   * Finds a page of entities.
   *
   * @param pageable pagination information
   * @return a page of entities
   */
  public Page<E> findAll(Pageable pageable) {
    return repository.findAll(pageable);
  }

  /**
   * Finds all entities.
   *
   * @return a list of all entities
   */
  public List<E> findAll() {
    return repository.findAll();
  }

  /**
   * Deletes an entity by its ID.
   *
   * @param id the ID of the entity to delete
   * @throws EntityNotFoundException if the entity with the given ID does not exist
   */
  @Transactional
  public void deleteById(ID id) {
    var entity = repository.findById(id).orElseThrow(EntityNotFoundException::new);

    repository.delete(entity);
  }
}
