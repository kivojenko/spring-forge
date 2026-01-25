package com.kivojenko.spring.forge.jpa.contract;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Abstract base class for generated services.
 * Provides standard business logic for CRUD operations.
 *
 * @param <E> the entity type
 * @param <ID> the ID type
 * @param <R> the repository type
 */
public abstract class ForgeService<E, ID, R extends JpaRepository<E, ID>> {
    /**
     * The repository used for data access.
     */
    @Autowired
    protected R repository;

    /**
     * Hook to modify or validate an entity before it is saved.
     * @param entity the entity to fix
     * @return the fixed entity
     */
    public E fixParameters(E entity) {
        return entity;
    }

    /**
     * Gets an entity by its ID.
     * @param id the ID of the entity
     * @return the entity
     * @throws EntityNotFoundException if not found
     */
    public E getById(ID id) {
        return repository.findById(id).orElseThrow(EntityNotFoundException::new);
    }

    /**
     * Checks if an entity exists by its ID.
     * @param id the ID of the entity
     * @return true if exists, false otherwise
     */
    public boolean exists(ID id) {
        return repository.existsById(id);
    }

    /**
     * Creates a new entity.
     * @param entity the entity to create
     * @return the created entity
     */
    @Transactional
    public E create(E entity) {
        entity = fixParameters(entity);
        return repository.save(entity);
    }

    /**
     * Updates an existing entity.
     * @param id the ID of the entity to update
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
        return repository.save(entity);
    }

    /**
     * Abstract method to set the ID of an entity.
     * @param entity the entity
     * @param id the ID to set
     */
    protected abstract void setId(E entity, ID id);


    /**
     * Returns the total number of entities.
     * @return the count
     */
    public long count() {
        return repository.count();
    }

    /**
     * Returns a page of entities.
     * @param pageable the pagination information
     * @return a page of entities
     */
    public Iterable<E> findAll(Pageable pageable) {
        return repository.findAll(pageable);
    }

    /**
     * Deletes an entity by its ID.
     * @param id the ID of the entity to delete
     * @throws EntityNotFoundException if not found
     */
    @Transactional
    public void deleteById(ID id) {
        var entity = repository.findById(id).orElseThrow(EntityNotFoundException::new);
        repository.delete(entity);
    }
}
