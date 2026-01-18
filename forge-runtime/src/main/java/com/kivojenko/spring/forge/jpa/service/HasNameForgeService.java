package com.kivojenko.spring.forge.jpa.service;

import com.kivojenko.spring.forge.jpa.contract.HasName;
import com.kivojenko.spring.forge.jpa.repository.HasNameRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Extension of {@link ForgeService} for entities implementing {@link HasName}.
 * Adds business logic for name-based operations.
 *
 * @param <E>  the entity type
 * @param <ID> the ID type
 * @param <R>  the repository type
 */
public abstract class HasNameForgeService<E extends HasName, ID,
        R extends JpaRepository<E, ID> & HasNameRepository<E>> extends
        ForgeService<E, ID, R>
{

    @Override
    @Transactional
    public E create(E entity) {
        if (repository.existsByName(entity.getName())) {
            throw new IllegalArgumentException("Entity with name " + entity.getName() + " already exists");
        }
        return super.create(entity);
    }

}
