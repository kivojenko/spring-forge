package com.kivojenko.spring.forge.jpa.controller;


import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.web.bind.annotation.RestController;

@RestController
public abstract class ForgeController<E, ID, R extends JpaRepository<E, ID>> implements CrudController<E, ID> {
    @Autowired
    protected R repository;


    @Override
    public long count() {
        return repository.count();
    }

    @Override
    public Iterable<E> findAll(int page, int size) {
        return repository.findAll(PageRequest.of(page, size));
    }

    @Override
    public E create(E entity) {
        return repository.save(entity);
    }

    @Override
    public E getById(ID id) {
        return repository.findById(id).orElseThrow(EntityNotFoundException::new);
    }

    @Override
    public E update(ID id, E entity) {
        if (!repository.existsById(id)) {
            throw new EntityNotFoundException();
        }
        setId(entity, id);
        return repository.save(entity);
    }

    @Override
    public void delete(ID id) {
        repository.deleteById(id);
    }

    protected abstract void setId(E entity, ID id);
}
