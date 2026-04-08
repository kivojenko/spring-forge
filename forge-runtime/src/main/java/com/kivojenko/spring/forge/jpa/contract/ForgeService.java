package com.kivojenko.spring.forge.jpa.contract;

import com.kivojenko.spring.forge.jpa.contract.persistence.ForgePersistenceHook;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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

  @Autowired(required = false)
  protected List<ForgePersistenceHook<? super E>> hooks = List.of();

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
   * Executes {@link ForgePersistenceHook#beforeCreate(Object)} and {@link ForgePersistenceHook#afterCreate(Object)} hooks.
   *
   * @param entity the entity to create
   * @return the saved entity
   */
  @Transactional
  public E create(E entity) {
    entity = fixParameters(entity);

    for (var hook : hooks) {
      hook.beforeCreate(entity);
    }
    E saved = repository.save(entity);
    hooks.forEach(h -> h.afterCreate(saved));

    return saved;
  }

  /**
   * Updates an existing entity.
   * Executes {@link ForgePersistenceHook#beforeUpdate(Object)} and {@link ForgePersistenceHook#afterUpdate(Object)} hooks.
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

    for (var hook : hooks) {
      hook.beforeUpdate(entity);
    }
    E saved = repository.save(entity);
    hooks.forEach(h -> h.afterUpdate(saved));

    return saved;
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
   * Executes {@link ForgePersistenceHook#beforeDelete(Object)} and {@link ForgePersistenceHook#afterDelete(Object)} hooks.
   *
   * @param id the ID of the entity to delete
   * @throws EntityNotFoundException if the entity with the given ID does not exist
   */
  @Transactional
  public void deleteById(ID id) {
    var entity = repository.findById(id).orElseThrow(EntityNotFoundException::new);

    hooks.forEach(hook -> hook.beforeDelete(entity));
    repository.delete(entity);
    hooks.forEach(h -> h.afterDelete(entity));

  }
}
