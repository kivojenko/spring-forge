package com.kivojenko.spring.forge.jpa.service;

import com.kivojenko.spring.forge.jpa.contract.HasName;
import com.kivojenko.spring.forge.jpa.repository.HasNameRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Extension of {@link HasNameForgeService} that adds a "get or create" operation.
 *
 * @param <E> the entity type
 * @param <ID> the ID type
 * @param <R> the repository type
 */
public abstract class HasNameForgeServiceWithGetOrCreate<E extends HasName, ID,
        R extends JpaRepository<E, ID> & HasNameRepository<E>> extends
        HasNameForgeService<E, ID, R> {

    /**
     * Gets an existing entity by name (case-insensitive) or creates a new one.
     *
     * @param name the name of the entity
     * @return the existing or newly created entity
     */
    @Transactional
    public E getOrCreate(String name) {
        return repository.findByNameIgnoreCase(name).orElseGet(() -> createSafely(name));
    }

    private E createSafely(String name) {
        try {
            return repository.save(create(name));
        } catch (DataIntegrityViolationException e) {
            return repository.findByNameIgnoreCase(name).orElseThrow();
        }
    }

    /**
     * Abstract method to create a new entity instance with the given name.
     * @param name the name for the new entity
     * @return the new entity instance
     */
    protected abstract E create(String name);
}
