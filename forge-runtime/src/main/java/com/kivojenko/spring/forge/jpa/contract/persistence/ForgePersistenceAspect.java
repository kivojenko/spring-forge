package com.kivojenko.spring.forge.jpa.contract.persistence;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;

import com.kivojenko.spring.forge.jpa.contract.ForgeService;

/**
 * Interface for persistence hooks that are automatically executed before and after CRUD operations via AOP.
 * To use a hook, implement this interface as a Spring {@code @Bean} or {@code @Component}
 * for a specific entity type. The {@code ForgePersistenceAspect} will automatically
 * discover and execute it.
 *
 * @param <E> the entity type
 */
@Aspect
public abstract class ForgePersistenceAspect<E> {

  protected abstract Class<E> entityType();

  /**
   * Executed before an entity is created.
   *
   * @param entity the entity to be created
   */
  @SuppressWarnings("unchecked")
  @Before("execution(* com.kivojenko.spring.forge.jpa.contract.ForgeService+.create(..)) && args(entity)")
  public void onBeforeCreate(Object entity) {
    if (!entityType().isInstance(entity)) {
      return;
    }
    beforeCreate((E) entity);
  }

  public void beforeCreate(E entity) {}

  /**
   * Executed after an entity is created.
   *
   * @param entity the created entity
   */
  @SuppressWarnings("unchecked")
  @After("execution(* com.kivojenko.spring.forge.jpa.contract.ForgeService+.create(..)) && args(entity)")
  public void onAfterCreate(Object entity) {
    if (!entityType().isInstance(entity)) {
      return;
    }
    afterCreate((E) entity);
  }

  public void afterCreate(E entity) {}

  /**
   * Executed before a sub-entity is added to a main entity.
   *
   * <p>Matches the generated association-linking methods, which are named
   * {@code addNew<Field>} (create + attach) or {@code addExisting<Field>} (attach by ID) —
   * never a bare {@code *Add*}.
   *
   * @param joinPoint the join point
   */
  @Before("within(com.kivojenko.spring.forge.jpa.contract.ForgeService+) && (execution(* addNew*(..)) || execution(* addExisting*(..)))")
  public void onBeforeAdd(JoinPoint joinPoint) {
    if (joinPoint.getThis() instanceof ForgeService<?, ?, ?> service) {
      Object[] args = joinPoint.getArgs();
      if (args.length >= 2) {
        Object mainEntity = args[0];
        Object subEntity = args[1];
        if (!shouldHandle(service, mainEntity)) {
          return;
        }
        beforeAdd(mainEntity, subEntity);
      }
    }
  }

  private boolean shouldHandle(ForgeService<?, ?, ?> service, Object firstArg) {
    if (entityType().isInstance(firstArg)) {
      return true;
    }
    return service.getEntityClass().equals(entityType());
  }

  public void beforeAdd(Object mainEntity, Object subEntity) {}

  /**
   * Executed after a sub-entity is added to a main entity.
   *
   * <p>See {@link #onBeforeAdd(JoinPoint)} for which generated methods this matches.
   *
   * @param joinPoint the join point
   */
  @After("within(com.kivojenko.spring.forge.jpa.contract.ForgeService+) && (execution(* addNew*(..)) || execution(* addExisting*(..)))")
  public void onAfterAdd(JoinPoint joinPoint) {
    if (joinPoint.getThis() instanceof ForgeService<?, ?, ?> service) {
      Object[] args = joinPoint.getArgs();
      if (args.length >= 2) {
        Object mainEntity = args[0];
        Object subEntity = args[1];
        if (!shouldHandle(service, mainEntity)) {
          return;
        }
        afterAdd(mainEntity, subEntity);
      }
    }
  }

  public void afterAdd(Object mainEntity, Object subEntity) {}

  /**
   * Executed before an entity is updated (full update via {@code PUT}).
   *
   * @param entity the entity to be updated
   */
  @SuppressWarnings("unchecked")
  @Before("execution(* com.kivojenko.spring.forge.jpa.contract.ForgeService+.update(..)) && args(.., entity)")
  public void onBeforeUpdate(Object entity) {
    if (!entityType().isInstance(entity)) {
      return;
    }
    beforeUpdate((E) entity);
  }

  public void beforeUpdate(E entity) {}

  /**
   * Executed after an entity is updated (full update via {@code PUT}).
   *
   * @param entity the updated entity
   */
  @SuppressWarnings("unchecked")
  @After("execution(* com.kivojenko.spring.forge.jpa.contract.ForgeService+.update(..)) && args(.., entity)")
  public void onAfterUpdate(Object entity) {
    if (!entityType().isInstance(entity)) {
      return;
    }
    afterUpdate((E) entity);
  }

  public void afterUpdate(E entity) {}

  /**
   * Executed after an entity is partially updated via {@code PATCH}.
   *
   * <p>{@code ForgeService.patch(ID, Map)} does not receive the entity as an argument
   * (only its ID and a field map), so there is no equivalent "before" hook with the
   * pre-patch entity available through AOP. The saved entity is forwarded to
   * {@link #afterUpdate(Object)} so that any subclass auditing updates also picks up
   * partial updates without needing a separate override.
   *
   * @param entity the patched entity, as returned by {@code patch(..)}
   */
  @SuppressWarnings("unchecked")
  @AfterReturning(pointcut = "execution(* com.kivojenko.spring.forge.jpa.contract.ForgeService+.patch(..))", returning = "entity")
  public void onAfterPatch(Object entity) {
    if (!entityType().isInstance(entity)) {
      return;
    }
    afterUpdate((E) entity);
  }

  /**
   * Executed before an entity is deleted.
   *
   * @param entity the entity to be deleted
   */
  @SuppressWarnings("unchecked")
  @Before("execution(* org.springframework.data.jpa.repository.JpaRepository+.delete(..)) && args(entity)")
  public void onBeforeDelete(Object entity) {
    if (!entityType().isInstance(entity)) {
      return;
    }
    beforeDelete((E) entity);
  }

  public void beforeDelete(E entity) {}

  /**
   * Executed after an entity is deleted.
   *
   * @param entity the deleted entity
   */
  @SuppressWarnings("unchecked")
  @After("execution(* org.springframework.data.jpa.repository.JpaRepository+.delete(..)) && args(entity)")
  public void onAfterDelete(Object entity) {
    if (!entityType().isInstance(entity)) {
      return;
    }
    afterDelete((E) entity);
  }

  public void afterDelete(E entity) {}

  /**
   * Executed before a sub-entity is removed from a main entity.
   *
   * <p>Matches the generated association-unlinking methods, which are named
   * {@code remove<Field>}, {@code removeRelationWith<Field>} (One-to-Many / Many-to-Many),
   * or {@code unlink<Field>} (Many-to-One / One-to-One) — never a bare {@code *Remove*}.
   *
   * @param joinPoint the join point
   */
  @Before("within(com.kivojenko.spring.forge.jpa.contract.ForgeService+) && (execution(* remove*(..)) || execution(* unlink*(..)))")
  public void onBeforeDeleteSub(JoinPoint joinPoint) {
    if (joinPoint.getThis() instanceof ForgeService<?, ?, ?> service) {
      Object[] args = joinPoint.getArgs();
      if (args.length >= 2) {
        Object mainEntity = args[0];
        Object subEntity = args[1];
        if (!shouldHandle(service, mainEntity)) {
          return;
        }
        beforeDelete(mainEntity, subEntity);
      }
    }
  }

  public void beforeDelete(Object mainEntity, Object subEntity) {}

  /**
   * Executed after a sub-entity is removed from a main entity.
   *
   * <p>See {@link #onBeforeDeleteSub(JoinPoint)} for which generated methods this matches.
   *
   * @param joinPoint the join point
   */
  @After("within(com.kivojenko.spring.forge.jpa.contract.ForgeService+) && (execution(* remove*(..)) || execution(* unlink*(..)))")
  public void onAfterDeleteSub(JoinPoint joinPoint) {
    if (joinPoint.getThis() instanceof ForgeService<?, ?, ?> service) {
      Object[] args = joinPoint.getArgs();
      if (args.length >= 2) {
        Object mainEntity = args[0];
        Object subEntity = args[1];
        if (!shouldHandle(service, mainEntity)) {
          return;
        }
        afterDelete(mainEntity, subEntity);
      }
    }
  }

  public void afterDelete(Object mainEntity, Object subEntity) {}
}
