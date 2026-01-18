package com.kivojenko.spring.forge.jpa.controller;


import com.kivojenko.spring.forge.jpa.contract.HasName;
import com.kivojenko.spring.forge.jpa.repository.HasNameRepository;
import com.kivojenko.spring.forge.jpa.service.ForgeServiceWithGetOrCreate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@RestController
public abstract class ForgeControllerWithServiceWithGetOrCreate<E extends HasName, ID,
        R extends JpaRepository<E, ID> & HasNameRepository<E>,
        S extends ForgeServiceWithGetOrCreate<E, ID, R>> extends
        ForgeControllerWithService<E, ID, R, S> {

    /**
     * Gets an existing entity by name or creates a new one if it doesn't exist.
     *
     * @param name the name of the entity
     * @return the existing or newly created entity
     */
    @PostMapping("/get-or-create")
    public E getOrCreate(@RequestParam String name) {
        return service.getOrCreate(name);
    }
}
