package com.kivojenko.spring.forge.jpa.contract.persistence;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
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
   * @param mainEntity the main entity service
   * @param subEntity  the sub-entity to be added
   */
  @SuppressWarnings("unchecked")
  @Before(value = "execution(* com.kivojenko.spring.forge.jpa.contract.ForgeService+.*Add*(..)) && args(mainEntity, subEntity)", argNames = "joinPoint,mainEntity,subEntity")
  public void onBeforeAdd(JoinPoint joinPoint, Object mainEntity, Object subEntity) {
    if (!shouldHandle(joinPoint, mainEntity)) {
      return;
    }
    beforeAdd(mainEntity, subEntity);
  }

  private boolean shouldHandle(JoinPoint joinPoint, Object firstArg) {
    if (entityType().isInstance(firstArg)) {
      return true;
    }
    if (joinPoint.getTarget() instanceof ForgeService<?, ?, ?> service) {
      return service.getEntityClass().equals(entityType());
    }
    return false;
  }

  public void beforeAdd(Object mainEntity, Object subEntity) {}

  /**
   * Executed after a sub-entity is added to a main entity.
   *
   * @param mainEntity the main entity service
   * @param subEntity  the added sub-entity
   */
  @SuppressWarnings("unchecked")
  @After(value = "execution(* com.kivojenko.spring.forge.jpa.contract.ForgeService+.*Add*(..)) && args(mainEntity, subEntity)", argNames = "joinPoint,mainEntity,subEntity")
  public void onAfterAdd(JoinPoint joinPoint, Object mainEntity, Object subEntity) {
    if (!shouldHandle(joinPoint, mainEntity)) {
      return;
    }
    afterAdd((E) mainEntity, subEntity);
  }

  public void afterAdd(E mainEntity, Object subEntity) {}

  /**
   * Executed before an entity is updated.
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
   * Executed after an entity is updated.
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
   * Executed before a sub-entity is deleted from a main entity.
   *
   * @param mainEntity the main entity service
   * @param subEntity  the sub-entity to be deleted
   */
  @SuppressWarnings("unchecked")
  @Before(value = "execution(* com.kivojenko.spring.forge.jpa.contract.ForgeService+.*Remove*(..)) && args(mainEntity, subEntity)", argNames = "joinPoint,mainEntity,subEntity")
  public void onBeforeDeleteSub(JoinPoint joinPoint, Object mainEntity, Object subEntity) {
    if (!shouldHandle(joinPoint, mainEntity)) {
      return;
    }
    beforeDelete(mainEntity, subEntity);
  }

  public void beforeDelete(Object mainEntity, Object subEntity) {}

  /**
   * Executed after a sub-entity is deleted from a main entity.
   *
   * @param subEntity the deleted sub-entity
   */
  @SuppressWarnings("unchecked")
  @After(value = "execution(* com.kivojenko.spring.forge.jpa.contract.ForgeService+.*Remove*(..)) && args(mainEntity, subEntity)", argNames = "joinPoint,mainEntity,subEntity")
  public void onAfterDeleteSub(JoinPoint joinPoint, Object mainEntity, Object subEntity) {
    if (!shouldHandle(joinPoint, mainEntity)) {
      return;
    }
    afterDelete(mainEntity, subEntity);
  }

  public void afterDelete(Object mainEntity, Object subEntity) {}
}
