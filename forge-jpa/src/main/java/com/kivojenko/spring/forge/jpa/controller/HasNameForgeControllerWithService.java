package com.kivojenko.spring.forge.jpa.controller;


import com.kivojenko.spring.forge.jpa.contract.HasName;
import com.kivojenko.spring.forge.jpa.repository.HasNameRepository;
import com.kivojenko.spring.forge.jpa.service.HasNameForgeService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public abstract class HasNameForgeControllerWithService<E extends HasName, ID,
        R extends JpaRepository<E, ID> & HasNameRepository<E>, S extends HasNameForgeService<E, ID, R>> extends
        ForgeControllerWithService<E, ID, R, S> {
    @GetMapping(params = {"name"})
    public Iterable<E> getAll(@RequestParam(defaultValue = "0", name = "page", required = false) int page,
                              @RequestParam(defaultValue = "250", name = "size", required = false) int size,
                              @RequestParam(required = false, name = "name") String name) {

        var pageable = PageRequest.of(page, size);
        return service.findAll(pageable, name);
    }
}
