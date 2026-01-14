package com.kivojenko.spring.forge.jpa.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * Base interface for generated CRUD controllers.
 * Provides standard endpoints for basic CRUD operations.
 *
 * @param <E> the entity type
 * @param <ID> the ID type
 */
public interface CrudController<E, ID> {

    /**
     * Returns the total number of entities.
     * @return the count
     */
    @GetMapping("/count")
    long count();

    /**
     * Returns a page of entities.
     * @param page the page number (0-indexed)
     * @param size the page size
     * @return an iterable of entities
     */
    @GetMapping
    Iterable<E> findAll(
            @RequestParam(defaultValue = "0", name = "page") int page,
            @RequestParam(defaultValue = "25", name = "size") int size
    );

    /**
     * Creates a new entity.
     * @param entity the entity to create
     * @return the created entity
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    E create(@Valid @RequestBody E entity);

    /**
     * Gets an entity by its ID.
     * @param id the ID of the entity
     * @return the entity
     */
    @GetMapping("/{id}")
    E getById(@PathVariable("id") ID id);

    /**
     * Checks if an entity exists by its ID.
     * @param id the ID of the entity
     * @return true if exists, false otherwise
     */
    @RequestMapping(method = RequestMethod.HEAD, path = "/{id}")
    boolean exists(@PathVariable("id") ID id);

    /**
     * Updates an existing entity.
     * @param id the ID of the entity to update
     * @param entity the updated entity data
     * @return the updated entity
     */
    @PutMapping("/{id}")
    E update(@PathVariable("id") ID id, @RequestBody E entity);

    /**
     * Deletes an entity by its ID.
     * @param id the ID of the entity to delete
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable("id") ID id);
}
