package com.kivojenko.spring.forge.jpa.repository;

import com.kivojenko.spring.forge.jpa.contract.HasName;
import org.springframework.data.domain.PageRequest;

import java.util.Optional;

public interface HasNameRepository<E extends HasName> {
    boolean existsByName(String name);

    Optional<E> findByName(String name);

    Iterable<E> findAllByNameContainingIgnoreCase(String name, PageRequest pageable);

    Optional<E> findByNameIgnoreCase(String name);
}
