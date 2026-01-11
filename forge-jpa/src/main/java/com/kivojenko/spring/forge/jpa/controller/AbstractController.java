package com.kivojenko.spring.forge.jpa.controller;


import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.web.bind.annotation.*;

@RestController
public abstract class AbstractController<E, _ID, R extends JpaRepository<E, _ID>> {
    @Autowired
    protected R repository;

    @GetMapping("/count")
    public long count() {
        return repository.count();
    }

    @GetMapping
    public Iterable<E> getAll(
            @RequestParam(defaultValue = "0", name = "page") int page,
            @RequestParam(defaultValue = "25", name = "size") int size) {
        var pageable = PageRequest.of(page, size);
        return repository.findAll(pageable);
    }

    @PostMapping
    @ResponseStatus(code = org.springframework.http.HttpStatus.CREATED)
    public E create(@Valid @RequestBody E entity) {
        return repository.save(entity);
    }

    @GetMapping("/{id}")
    public E getById(@PathVariable("id") _ID id) {
        return repository.findById(id).orElseThrow();
    }

    protected abstract void setId(E entity, _ID id);

    @PutMapping("/{id}")
    public E update(@PathVariable("id") _ID id, @RequestBody E entity) {
        setId(entity, id);
        return repository.save(entity);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(code = org.springframework.http.HttpStatus.NO_CONTENT)
    public void delete(@PathVariable("id") _ID id) {
        var entity = repository.findById(id).orElseThrow();
        repository.delete(entity);
    }
}
