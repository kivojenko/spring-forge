package com.kivojenko.spring.forge.jpa.controller;

import com.kivojenko.spring.forge.jpa.contract.HasName;
import com.kivojenko.spring.forge.jpa.repository.HasNameRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;


/**
 * Extension of {@link ForgeController} for entities implementing {@link HasName}.
 * Adds an endpoint to search entities by name.
 *
 * @param <E>   the entity type
 * @param <_ID> the ID type
 * @param <R>   the repository type
 */
public abstract class HasNameForgeController<E extends HasName, _ID,
        R extends JpaRepository<E, _ID> & HasNameRepository<E>> extends
        ForgeController<E, _ID, R>
{

    /**
     * Returns a page of entities, optionally filtered by name.
     *
     * @param pageable the pagination parameters
     * @param name     the name to filter by (case-insensitive, contains)
     * @return an iterable of entities
     */
    @GetMapping(params = {"name"})
    public Iterable<E> getAll(@PageableDefault(size = 25)Pageable pageable, @RequestParam(required = false, name = "name") String name) {
        if (name != null) {
            return repository.findAllByNameContainingIgnoreCase(name, pageable);
        }

        return repository.findAll(pageable);
    }
}
