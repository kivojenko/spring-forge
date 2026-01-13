package com.kivojenko.spring.forge.jpa.controller;


import com.kivojenko.spring.forge.jpa.contract.HasName;
import com.kivojenko.spring.forge.jpa.repository.HasNameRepository;
import com.kivojenko.spring.forge.jpa.service.HasNameForgeServiceWithGetOrCreate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public abstract class HasNameForgeControllerWithServiceWithGetOrCreate<E extends HasName, ID,
        R extends JpaRepository<E, ID> & HasNameRepository<E>,
        S extends HasNameForgeServiceWithGetOrCreate<E, ID, R>> extends
        HasNameForgeControllerWithService<E, ID, R, S> {

    @PostMapping("/get-or-create")
    public E getOrCreate(@RequestParam String name) {
        return service.getOrCreate(name);
    }
}
