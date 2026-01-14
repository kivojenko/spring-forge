package com.kivojenko.spring.forge.jpa.controller;

import com.kivojenko.spring.forge.jpa.contract.HasName;
import com.kivojenko.spring.forge.jpa.repository.HasNameRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;


/**
 * Extension of {@link ForgeController} for entities implementing {@link HasName}.
 * Adds an endpoint to search entities by name.
 *
 * @param <E> the entity type
 * @param <_ID> the ID type
 * @param <R> the repository type
 */
public abstract class HasNameForgeController<E extends HasName, _ID,
        R extends JpaRepository<E, _ID> & HasNameRepository<E>> extends ForgeController<E, _ID, R> {

    /**
     * Returns a page of entities, optionally filtered by name.
     *
     * @param page the page number (0-indexed)
     * @param size the page size
     * @param name the name to filter by (case-insensitive, contains)
     * @return an iterable of entities
     */
    @GetMapping(params = {"name"})
    public Iterable<E> getAll(@RequestParam(defaultValue = "0", name = "page", required = false) int page,
                              @RequestParam(defaultValue = "250", name = "size", required = false) int size,
                              @RequestParam(required = false, name = "name") String name) {

        var pageable = PageRequest.of(page, size);
        if (name != null) {
            return repository.findAllByNameContainingIgnoreCase(name, pageable);
        }

        return repository.findAll(pageable);
    }
}
