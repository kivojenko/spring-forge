package com.kivojenko.spring.forge.jpa.repository;

import com.kivojenko.spring.forge.jpa.contract.HasName;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface HasNameRepository<E extends HasName> {
    boolean existsByName(String name);
    boolean existsByNameIgnoreCase(String name);

    Optional<E> findByName(String name);
    Optional<E> findByNameIgnoreCase(String name);

    Iterable<E> findAllByNameContaining(String name);
    Iterable<E> findAllByNameContainingIgnoreCase(String name, Pageable pageable);
}
