package com.kivojenko.spring.forge.jpa.controller;


import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.web.bind.annotation.*;

@RestController
public abstract class AbstractController<_Entity, _ID, _Repository extends JpaRepository<_Entity, _ID>> {
    @Autowired
    protected _Repository repository;

    @GetMapping
    public Iterable<_Entity> getAll(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "250") int size) {
        var pageable = PageRequest.of(page, size);
        return repository.findAll(pageable);
    }

    @PostMapping
    @ResponseStatus(code = org.springframework.http.HttpStatus.CREATED)
    public _Entity create(@Valid @RequestBody _Entity entity) {
        return repository.save(entity);
    }

    @GetMapping("/{id}")
    public _Entity getById(@PathVariable _ID id) {
        return repository.findById(id).orElseThrow();
    }

    @GetMapping("/count")
    public long count() {
        return repository.count();
    }

    @PostMapping("/paged")
    public Page<_Entity> getPaged(
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "12") int size) {
        return repository.findAll(PageRequest.of(page, size));
    }

    protected abstract void setId(_Entity entity, _ID id);

    @PutMapping("/{id}")
    public _Entity update(@PathVariable _ID id, @RequestBody _Entity entity) {
        setId(entity, id);
        return repository.save(entity);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(code = org.springframework.http.HttpStatus.NO_CONTENT)
    public void delete(@PathVariable _ID id) {
        var entity = repository.findById(id).orElseThrow();
        repository.delete(entity);
    }
}
