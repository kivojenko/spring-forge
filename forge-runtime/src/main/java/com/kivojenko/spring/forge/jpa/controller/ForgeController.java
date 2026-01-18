package com.kivojenko.spring.forge.jpa.controller;


import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.web.bind.annotation.RestController;

/**
 * Abstract base implementation of {@link CrudController} that interacts directly with a {@link JpaRepository}.
 * Used when a service layer is not requested.
 *
 * @param <E> the entity type
 * @param <ID> the ID type
 * @param <R> the repository type
 */
@RestController
public abstract class ForgeController<E, ID, R extends JpaRepository<E, ID>> implements CrudController<E, ID> {
    /**
     * The repository used for data access.
     */
    @Autowired
    protected R repository;


    @Override
    public long count() {
        return repository.count();
    }

    @Override
    public Iterable<E> findAll(Pageable pageable) {
        return repository.findAll(pageable);
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
    public boolean exists(ID id) {
        return repository.existsById(id);
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
