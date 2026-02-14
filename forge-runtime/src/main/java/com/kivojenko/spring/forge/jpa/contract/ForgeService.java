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

  public E fixParameters(E entity) {
    return entity;
  }

  public E getById(ID id) {
    return repository.findById(id).orElseThrow(EntityNotFoundException::new);
  }

  public boolean exists(ID id) {
    return repository.existsById(id);
  }

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

  protected abstract void setId(E entity, ID id);


  public long count() {
    return repository.count();
  }

  public Page<E> findAll(Pageable pageable) {
    return repository.findAll(pageable);
  }

  public List<E> findAll() {
    return repository.findAll();
  }

  @Transactional
  public void deleteById(ID id) {
    var entity = repository.findById(id).orElseThrow(EntityNotFoundException::new);

    hooks.forEach(hook -> hook.beforeDelete(entity));
    repository.delete(entity);
    hooks.forEach(h -> h.afterDelete(entity));

  }
}
