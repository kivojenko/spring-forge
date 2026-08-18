package com.kivojenko.spring.forge.jpa.contract;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.Collection;
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

  // Optional: use the application's configured ObjectMapper when present, otherwise a
  // self-constructed default. PATCH map->entity conversion needs no app-specific config.
  @Autowired(required = false)
  protected ObjectMapper objectMapper;

  private ObjectMapper objectMapper() {
    if (objectMapper == null) {
      objectMapper = new ObjectMapper().findAndRegisterModules();
    }
    return objectMapper;
  }

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
   * Hook applied to the managed entity after PATCH fields have been merged in.
   *
   * <p>Unlike {@link #fixParameters(Object)} (used by create and PUT-update), this receives the
   * already-managed entity with the partial changes applied in place, so overrides can wire
   * bidirectional back-references or resolve shared values (e.g. de-duplicated translations)
   * without swapping the entity instance. Swapping the instance here would discard the merged
   * collections. Default implementation is a no-op.
   *
   * @param entity the managed entity with PATCH fields already applied
   * @return the entity to persist
   */
  protected E fixPatch(E entity) {
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
   * Applies a partial update to an existing (managed) entity. Fields present in the
   * {@code fields} map are applied to the persisted entity by name; the {@code id} field
   * (if present) is ignored, and fields absent from the map keep their current values.
   *
   * <p>Each incoming value is deserialized into the target field's <em>generic</em> type via
   * Jackson, so scalars, enums, nested objects and typed collections (e.g. {@code Set<ProductImage>})
   * become real instances rather than raw maps. Collection-typed fields are merged in place
   * (clear + add) so that {@code orphanRemoval}/all-delete-orphan mappings keep working and the
   * managed collection wrapper is preserved.
   *
   * <p>Entity-specific wiring (bidirectional back-references, shared-value resolution) should be
   * performed by overriding {@link #fixPatch(Object)}.
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
        continue; // unknown field - skip
      }
      field.setAccessible(true);

      // Deserialize into the field's generic type so collection elements and nested
      // objects become real entities (Set<ProductImage>), not raw LinkedHashMaps.
      JavaType javaType = objectMapper().getTypeFactory().constructType(field.getGenericType());
      Object value = objectMapper().convertValue(entry.getValue(), javaType);

      Object current = ReflectionUtils.getField(field, entity);
      if (current instanceof Collection<?> && value instanceof Collection<?>) {
        // Mutate the managed collection in place (never replace the instance, which would
        // break all-delete-orphan mappings and detach Hibernate's collection wrapper).
        //
        // This replaces children via orphan-removal delete + re-insert within one flush, where
        // Hibernate orders INSERTs before DELETEs. If the entity has a unique constraint on
        // (child, parent) and the replacement reuses an existing key, perform it as TWO PATCH
        // calls: first set the collection to [] (delete-only), then PATCH the new elements
        // (insert-only). That avoids the transient collision without an intra-call flush.
        @SuppressWarnings("unchecked")
        Collection<Object> target = (Collection<Object>) current;
        target.clear();
        target.addAll((Collection<?>) value);
      } else {
        if (value == null && field.getType().isPrimitive()) {
          continue; // cannot assign null to a primitive field
        }
        ReflectionUtils.setField(field, entity, value);
      }
    }

    var fixed = fixPatch(entity);
    return repository.save(fixed);
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
