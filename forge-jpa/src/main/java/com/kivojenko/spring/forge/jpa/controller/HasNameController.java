package com.kivojenko.spring.forge.jpa.controller;

import com.kivojenko.spring.forge.jpa.contract.HasName;
import com.kivojenko.spring.forge.jpa.repository.HasNameRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;


public abstract class HasNameController<E extends HasName, _ID,
        R extends JpaRepository<E, _ID> & HasNameRepository<E>> extends AbstractController<E, _ID, R> {

    @GetMapping
    public Iterable<E> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "250") int size,
            @RequestParam(required = false) String name) {

        var pageable = PageRequest.of(page, size);
        if (name != null) {
            return repository.findAllByNameContainingIgnoreCase(name, pageable);
        }

        return repository.findAll(pageable);
    }
}
