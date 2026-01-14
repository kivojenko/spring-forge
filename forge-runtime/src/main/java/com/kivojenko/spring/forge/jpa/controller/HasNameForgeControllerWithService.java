package com.kivojenko.spring.forge.jpa.controller;


import com.kivojenko.spring.forge.jpa.contract.HasName;
import com.kivojenko.spring.forge.jpa.repository.HasNameRepository;
import com.kivojenko.spring.forge.jpa.service.HasNameForgeService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Extension of {@link ForgeControllerWithService} for entities implementing {@link HasName}.
 * Adds an endpoint to search entities by name.
 *
 * @param <E> the entity type
 * @param <ID> the ID type
 * @param <R> the repository type
 * @param <S> the service type
 */
@RestController
public abstract class HasNameForgeControllerWithService<E extends HasName, ID,
        R extends JpaRepository<E, ID> & HasNameRepository<E>, S extends HasNameForgeService<E, ID, R>> extends
        ForgeControllerWithService<E, ID, R, S> {
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
        return service.findAll(pageable, name);
    }
}
