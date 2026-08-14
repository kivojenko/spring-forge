package com.kivojenko.spring.forge.jpa.contract;


import jakarta.validation.Valid;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * Concrete base implementation of controller that delegates to a {@link ForgeService}.
 * Extends {@link ForgeAbstractController} and provides standard CRUD operations including creation.
 *
 * @param <E>  the entity type
 * @param <ID> the ID type
 * @param <R>  the repository type
 * @param <S>  the service type
 */
@RestController
public abstract class ForgeController<E, ID, R extends JpaRepository<E, ID>, S extends ForgeService<E, ID, R>>
    extends ForgeAbstractController<E, ID, R, S> {

  /**
   * Delegates to {@link ForgeService#create(Object)}.
   *
   * @param entity the entity to create
   * @return the created entity
   */
  @PostMapping
  @ResponseStatus(code = HttpStatus.CREATED)
  public E create(@Valid @RequestBody E entity) {
    return service.create(entity);
  }
}
