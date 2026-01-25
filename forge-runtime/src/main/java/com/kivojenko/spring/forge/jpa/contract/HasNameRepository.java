package com.kivojenko.spring.forge.jpa.contract;

import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

/**
 * Fragment interface for repositories of entities implementing {@link HasName}.
 * Provides standard name-based query methods.
 *
 * @param <E> the entity type
 */
public interface HasNameRepository<E extends HasName> {
    /**
     * Checks if an entity with the exact name exists.
     * @param name the name to check
     * @return true if exists, false otherwise
     */
    boolean existsByName(String name);

    /**
     * Checks if an entity with the name exists, ignoring case.
     * @param name the name to check
     * @return true if exists, false otherwise
     */
    boolean existsByNameIgnoreCase(String name);

    /**
     * Finds an entity by its exact name.
     * @param name the name to search for
     * @return an optional containing the entity if found
     */
    Optional<E> findByName(String name);

    /**
     * Finds an entity by its name, ignoring case.
     * @param name the name to search for
     * @return an optional containing the entity if found
     */
    Optional<E> findByNameIgnoreCase(String name);

    /**
     * Finds all entities whose name contains the given string.
     * @param name the substring to search for
     * @return an iterable of matching entities
     */
    List<E> findAllByNameContaining(String name);

    /**
     * Finds a page of entities whose name contains the given string, ignoring case.
     * @param name the substring to search for
     * @param pageable the pagination information
     * @return an iterable of matching entities
     */
    List<E> findAllByNameContainingIgnoreCase(String name, Pageable pageable);
}
