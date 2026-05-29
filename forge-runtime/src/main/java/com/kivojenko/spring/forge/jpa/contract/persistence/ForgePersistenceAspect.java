package com.kivojenko.spring.forge.jpa.contract.persistence;

import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;

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

  /**
   * Executed before an entity is created.
   *
   * @param entity the entity to be created
   */
  @Before("execution(* com.kivojenko.spring.forge.jpa.contract.ForgeService+.create(..)) && args(entity)")
  public void onBeforeCreate(E entity) {
    beforeCreate(entity);
  }

  public void beforeCreate(E entity) {}

  /**
   * Executed after an entity is created.
   *
   * @param entity the created entity
   */
  @After("execution(* com.kivojenko.spring.forge.jpa.contract.ForgeService+.create(..)) && args(entity)")
  public void onAfterCreate(E entity) {
    afterCreate(entity);
  }

  public void afterCreate(E entity) {}

  /**
   * Executed before a sub-entity is added to a main entity.
   *
   * @param mainEntityId the main entity service
   * @param subEntity the sub-entity to be added
   */
  @Before(value = "execution(* com.kivojenko.spring.forge.jpa.contract.ForgeService+.*Add*(..)) && args(mainEntityId, subEntity)", argNames = "mainEntityId,subEntity")
  public void onBeforeAdd(Object mainEntityId, Object subEntity) {
    beforeAdd(mainEntityId, subEntity);
  }

  public void beforeAdd(Object mainEntity, Object subEntity) {}

  /**
   * Executed after a sub-entity is added to a main entity.
   *
   * @param mainEntity the main entity service
   * @param subEntity  the added sub-entity
   */
  @After(value = "execution(* com.kivojenko.spring.forge.jpa.contract.ForgeService+.*Add*(..)) && args(mainEntity, subEntity)", argNames = "mainEntity,subEntity")
  public void onAfterAdd(E mainEntity, Object subEntity) {
    afterAdd(mainEntity, subEntity);
  }

  public void afterAdd(E mainEntity, Object subEntity) {}

  /**
   * Executed before an entity is updated.
   *
   * @param entity the entity to be updated
   */
  @Before("execution(* com.kivojenko.spring.forge.jpa.contract.ForgeService+.update(..)) && args(.., entity)")
  public void onBeforeUpdate(E entity) {
    beforeUpdate(entity);
  }

  public void beforeUpdate(E entity) {}

  /**
   * Executed after an entity is updated.
   *
   * @param entity the updated entity
   */
  @After("execution(* com.kivojenko.spring.forge.jpa.contract.ForgeService+.update(..)) && args(.., entity)")
  public void onAfterUpdate(E entity) {
    afterUpdate(entity);
  }

  public void afterUpdate(E entity) {}

  /**
   * Executed before an entity is deleted.
   *
   * @param entity the entity to be deleted
   */
  @Before("execution(* org.springframework.data.jpa.repository.JpaRepository+.delete(..)) && args(entity)")
  public void onBeforeDelete(E entity) {
    beforeDelete(entity);
  }

  public void beforeDelete(E entity) {}

  /**
   * Executed after an entity is deleted.
   *
   * @param entity the deleted entity
   */
  @After("execution(* org.springframework.data.jpa.repository.JpaRepository+.delete(..)) && args(entity)")
  public void onAfterDelete(E entity) {
    afterDelete(entity);
  }

  public void afterDelete(E entity) {}

  /**
   * Executed before a sub-entity is deleted from a main entity.
   *
   * @param mainEntity the main entity service
   * @param subEntity  the sub-entity to be deleted
   */
  @Before(value = "execution(* com.kivojenko.spring.forge.jpa.contract.ForgeService+.*Remove*(..)) && args(mainEntity, subEntity)", argNames = "mainEntity,subEntity")
  public void onBeforeDeleteSub(E mainEntity, Object subEntity) {
    beforeDelete(mainEntity, subEntity);
  }

  public void beforeDelete(E mainEntity, Object subEntity) {}

  /**
   * Executed after a sub-entity is deleted from a main entity.
   *
   * @param subEntity the deleted sub-entity
   */
  @After(value = "execution(* com.kivojenko.spring.forge.jpa.contract.ForgeService+.*Remove*(..)) && args(mainEntity, subEntity)", argNames = "mainEntity,subEntity")
  public void onAfterDeleteSub(E mainEntity, Object subEntity) {
    afterDelete(mainEntity, subEntity);
  }

  public void afterDelete(E mainEntity, Object subEntity) {}
}
