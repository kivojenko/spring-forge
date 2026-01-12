package com.kivojenko.spring.forge.jpa.controller;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

public interface CrudController<E, ID> {

    @GetMapping("/count")
    long count();

    @GetMapping
    Iterable<E> findAll(
            @RequestParam(defaultValue = "0", name = "page") int page,
            @RequestParam(defaultValue = "25", name = "size") int size
    );

    @PostMapping
    @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    E create(@Valid @RequestBody E entity);

    @GetMapping("/{id}")
    E getById(@PathVariable("id") ID id);

    @PutMapping("/{id}")
    E update(@PathVariable("id") ID id, @RequestBody E entity);

    @DeleteMapping("/{id}")
    @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    void delete(@PathVariable("id") ID id);
}
