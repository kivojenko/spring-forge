package com.kivojenko.spring.forge.jpa.contract;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * Abstract base implementation of controller without POST create endpoints that delegates to a {@link ForgeService}.
 * Used when the entity is abstract or creation via POST is not supported.
 *
 * @param <E>  the entity type
 * @param <ID> the ID type
 * @param <R>  the repository type
 * @param <S>  the service type
 */
@RestController
public abstract class ForgeAbstractController<E, ID, R extends JpaRepository<E, ID>, S extends ForgeService<E, ID, R>> {

  @Autowired
  protected S service;

  /**
   * Delegates to {@link ForgeService#count()}.
   *
   * @return total number of entities
   */
  @GetMapping("/count")
  public long count() {
    return service.count();
  }

  /**
   * Delegates to {@link ForgeService#getById(Object)}.
   *
   * @param id the ID of the entity to retrieve
   * @return the found entity
   */
  @GetMapping("/{id}")
  public E getById(@PathVariable(name = "id") ID id) {
    return service.getById(id);
  }

  /**
   * Delegates to {@link ForgeService#exists(Object)}.
   *
   * @param id the ID of the entity to check
   * @return true if the entity exists, false otherwise
   */
  @RequestMapping(method = RequestMethod.HEAD, path = "/{id}")
  public boolean exists(@PathVariable(name = "id") ID id) {
    return service.exists(id);
  }

  /**
   * Delegates to {@link ForgeService#update(Object, Object)}.
   *
   * @param id the ID of the entity to update
   * @param entity the entity with updated fields
   * @return the updated entity
   */
  @PutMapping("/{id}")
  @ResponseStatus(code = HttpStatus.CREATED)
  public E update(@PathVariable(name = "id") ID id, @Valid @RequestBody E entity) {
    return service.update(id, entity);
  }

  /**
   * Delegates to {@link ForgeService#update(Object, Object)} using the HTTP PATCH method.
   * Semantically this behaves as an update operation; partial update semantics are
   * determined by the service implementation.
   *
   * @param id the ID of the entity to update
   * @param fields the fields to update
   * @return the updated entity
   */
  @PatchMapping("/{id}")
  public E patch(@PathVariable(name = "id") ID id, @RequestBody java.util.Map<String, Object> fields) {
    return service.patch(id, fields);
  }

  /**
   * Delegates to {@link ForgeService#deleteById(Object)}.
   *
   * @param id the ID of the entity to delete
   */
  @DeleteMapping("/{id}")
  @ResponseStatus(code = HttpStatus.NO_CONTENT)
  public void delete(@PathVariable(name = "id") ID id) {
    service.deleteById(id);
  }
}
