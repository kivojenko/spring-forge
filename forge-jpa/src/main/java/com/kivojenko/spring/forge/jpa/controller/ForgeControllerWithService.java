package com.kivojenko.spring.forge.jpa.controller;


import com.kivojenko.spring.forge.jpa.service.ForgeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.web.bind.annotation.RestController;

@RestController
public abstract class ForgeControllerWithService<E, ID, R extends JpaRepository<E, ID>, S extends ForgeService<E, ID,
        R>> implements
        CrudController<E, ID> {

    @Autowired
    protected S service;

    @Override
    public long count() {
        return service.count();
    }

    @Override
    public Iterable<E> findAll(int page, int size) {
        return service.findAll(PageRequest.of(page, size));
    }

    @Override
    public E create(E entity) {
        return service.create(entity);
    }

    @Override
    public E getById(ID id) {
        return service.getById(id);
    }

    @Override
    public E update(ID id, E entity) {
        return service.update(id, entity);
    }

    @Override
    public void delete(ID id) {
        service.deleteById(id);
    }
}
