package com.kivojenko.spring.forge.jpa.service;

import com.kivojenko.spring.forge.jpa.contract.HasName;
import com.kivojenko.spring.forge.jpa.repository.HasNameRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

public abstract class HasNameForgeServiceWithGetOrCreate<E extends HasName, ID,
        R extends JpaRepository<E, ID> & HasNameRepository<E>> extends
        HasNameForgeService<E, ID, R> {

    @Transactional
    public E getOrCreate(String name) {
        return repository.findByNameIgnoreCase(name).orElseGet(() -> createSafely(name));
    }

    private E createSafely(String name) {
        try {
            return repository.save(create(name));
        } catch (DataIntegrityViolationException e) {
            return repository.findByNameIgnoreCase(name).orElseThrow();
        }
    }

    protected abstract E create(String name);
}
