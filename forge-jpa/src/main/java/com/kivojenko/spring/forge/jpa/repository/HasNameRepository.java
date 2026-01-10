package com.kivojenko.spring.forge.jpa.repository;

import com.kivojenko.spring.forge.jpa.contract.HasName;

import java.util.Optional;

public interface HasNameRepository<E extends HasName> {
    boolean existsByName(String name);
    Optional<E> findByName(String name);
    Optional<E> findByNameIgnoreCase(String name);
}
