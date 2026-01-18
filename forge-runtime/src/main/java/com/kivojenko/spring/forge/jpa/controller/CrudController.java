package com.kivojenko.spring.forge.jpa.controller;

import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * Base interface for generated CRUD controllers.
 * Provides standard endpoints for basic CRUD operations.
 *
 * @param <E>  the entity type
 * @param <ID> the ID type
 */
public interface CrudController<E, ID> {

    /**
     * Returns the total number of entities.
     *
     * @return the count
     */
    @GetMapping("/count")
    long count();

    /**
     * Returns a page of entities.
     *
     * @param pageable the pagination parameters
     * @return an iterable of entities
     */
    @GetMapping
    Iterable<E> findAll(@PageableDefault(size = 25) Pageable pageable);

    /**
     * Creates a new entity.
     *
     * @param entity the entity to create
     * @return the created entity
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    E create(@Valid @RequestBody E entity);

    /**
     * Gets an entity by its ID.
     *
     * @param id the ID of the entity
     * @return the entity
     */
    @GetMapping("/{id}")
    E getById(@PathVariable ID id);

    /**
     * Checks if an entity exists by its ID.
     *
     * @param id the ID of the entity
     * @return true if exists, false otherwise
     */
    @RequestMapping(method = RequestMethod.HEAD, path = "/{id}")
    boolean exists(@PathVariable ID id);

    /**
     * Updates an existing entity.
     *
     * @param id     the ID of the entity to update
     * @param entity the updated entity data
     * @return the updated entity
     */
    @PutMapping("/{id}")
    E update(@PathVariable ID id, @RequestBody E entity);

    /**
     * Deletes an entity by its ID.
     *
     * @param id the ID of the entity to delete
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable ID id);
}
