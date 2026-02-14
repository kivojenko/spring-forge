package com.kivojenko.spring.forge.jpa.contract;


import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * Abstract base implementation of controller that delegates to a {@link ForgeService}.
 * Used when a service layer is requested.
 *
 * @param <E>  the entity type
 * @param <ID> the ID type
 * @param <R>  the repository type
 * @param <S>  the service type
 */
@RestController
public abstract class ForgeController<E, ID, R extends JpaRepository<E, ID>, S extends ForgeService<E, ID, R>> {

  @Autowired
  protected S service;

  @GetMapping("/count")
  public long count() {
    return service.count();
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public E create(@Valid @RequestBody E entity) {
    return service.create(entity);
  }

  @GetMapping("/{id}")
  public E getById(@PathVariable(name = "id") ID id) {
    return service.getById(id);
  }

  @RequestMapping(method = RequestMethod.HEAD, path = "/{id}")
  public boolean exists(@PathVariable(name = "id") ID id) {
    return service.exists(id);
  }

  @PutMapping("/{id}")
  @ResponseStatus(code = HttpStatus.CREATED)
  public E update(@PathVariable(name = "id") ID id, @Valid @RequestBody E entity) {
    return service.update(id, entity);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(code = HttpStatus.NO_CONTENT)
  public void delete(@PathVariable(name = "id") ID id) {
    service.deleteById(id);
  }
}
