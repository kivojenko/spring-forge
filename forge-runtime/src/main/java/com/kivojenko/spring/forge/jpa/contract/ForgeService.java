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
  /**
   * The repository used for data access.
   */
  @Autowired
  protected R repository;
  /**
   * List of persistence hooks to be executed during CRUD operations.
   */
  @Autowired(required = false)
  protected List<ForgePersistenceHook<? super E>> hooks = List.of();

  /**
   * Hook to modify or validate an entity before it is saved.
   *
   * @param entity the entity to fix
   * @return the fixed entity
   */
  public E fixParameters(E entity) {
    return entity;
  }

  /**
   * Gets an entity by its ID.
   *
   * @param id the ID of the entity
   * @return the entity
   * @throws EntityNotFoundException if not found
   */
  public E getById(ID id) {
    return repository.findById(id).orElseThrow(EntityNotFoundException::new);
  }

  /**
   * Checks if an entity exists by its ID.
   *
   * @param id the ID of the entity
   * @return true if exists, false otherwise
   */
  public boolean exists(ID id) {
    return repository.existsById(id);
  }

  /**
   * Creates a new entity.
   *
   * @param entity the entity to create
   * @return the created entity
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
   *
   * @param id     the ID of the entity to update
   * @param entity the updated entity data
   * @return the updated entity
   * @throws EntityNotFoundException if not found
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
   * Abstract method to set the ID of an entity.
   *
   * @param entity the entity
   * @param id     the ID to set
   */
  protected abstract void setId(E entity, ID id);


  /**
   * Returns the total number of entities.
   *
   * @return the count
   */
  public long count() {
    return repository.count();
  }

  /**
   * Returns a page of entities.
   *
   * @param pageable the pagination information
   * @return a page of entities
   */
  public Page<E> findAll(Pageable pageable) {
    return repository.findAll(pageable);
  }

  /**
   * Returns all entities.
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
   * @throws EntityNotFoundException if not found
   */
  @Transactional
  public void deleteById(ID id) {
    var entity = repository.findById(id).orElseThrow(EntityNotFoundException::new);

    hooks.forEach(hook -> hook.beforeDelete(entity));
    repository.delete(entity);
    hooks.forEach(h -> h.afterDelete(entity));

  }
}
