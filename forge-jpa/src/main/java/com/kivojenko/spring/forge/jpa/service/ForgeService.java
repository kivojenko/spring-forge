package com.kivojenko.spring.forge.jpa.service;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

public abstract class ForgeService<E, ID, R extends JpaRepository<E, ID>> {
    @Autowired
    protected R repository;

    public E fixParameters(E entity) {
        return entity;
    }

    public E getById(ID id) {
        return repository.findById(id).orElseThrow(EntityNotFoundException::new);
    }

    public boolean exists(ID id) {
        return repository.existsById(id);
    }

    @Transactional
    public E create(E entity) {
        entity = fixParameters(entity);
        return repository.save(entity);
    }

    @Transactional
    public E update(ID id, E entity) {
        if (!repository.existsById(id)) {
            throw new EntityNotFoundException();
        }
        setId(entity, id);
        return repository.save(entity);
    }

    protected abstract void setId(E entity, ID id);


    public long count() {
        return repository.count();
    }

    public Iterable<E> findAll(Pageable pageable) {
        return repository.findAll(pageable);
    }

    @Transactional
    public void deleteById(ID id) {
        var entity = repository.findById(id).orElseThrow(EntityNotFoundException::new);
        repository.delete(entity);
    }
}
