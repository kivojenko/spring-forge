package com.kivojenko.spring.forge.jpa.service;

import com.kivojenko.spring.forge.jpa.contract.HasName;
import com.kivojenko.spring.forge.jpa.repository.HasNameRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Extension of {@link ForgeService} for entities implementing {@link HasName}.
 * Adds business logic for name-based operations.
 *
 * @param <E> the entity type
 * @param <ID> the ID type
 * @param <R> the repository type
 */
public abstract class HasNameForgeService<E extends HasName, ID,
        R extends JpaRepository<E, ID> & HasNameRepository<E>> extends
        ForgeService<E, ID, R> {

    /**
     * Returns a page of entities, optionally filtered by name.
     *
     * @param pageable the pagination information
     * @param name the name to filter by (case-insensitive, contains)
     * @return a page of entities
     */
    public Iterable<E> findAll(Pageable pageable, String name) {
        if (name != null) {
            return repository.findAllByNameContainingIgnoreCase(name, pageable);
        }

        return super.findAll(pageable);
    }

    @Override
    @Transactional
    public E create(E entity) {
        if (repository.existsByName(entity.getName())) {
            throw new IllegalArgumentException("Entity with name " + entity.getName() + " already exists");
        }
        return super.create(entity);
    }

    @Override
    @Transactional
    public E update(ID id, E entity) {
        if (!repository.existsById(id)) {
            throw new EntityNotFoundException();
        }

        return super.create(entity);
    }
}
