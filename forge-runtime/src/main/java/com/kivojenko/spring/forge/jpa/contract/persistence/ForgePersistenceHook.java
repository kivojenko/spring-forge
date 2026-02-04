package com.kivojenko.spring.forge.jpa.contract.persistence;

/**
 * Interface for persistence hooks that can be executed before and after CRUD operations.
 *
 * @param <E> the entity type
 */
public interface ForgePersistenceHook<E> {

  /**
   * Executed before an entity is created.
   *
   * @param entity the entity to be created
   */
  default void beforeCreate(E entity) {}

  /**
   * Executed after an entity is created.
   *
   * @param entity the created entity
   */
  default void afterCreate(E entity) {}

  /**
   * Executed before a sub-entity is added to a main entity.
   *
   * @param mainEntity the main entity
   * @param subEntity  the sub-entity to be added
   */
  default void beforeAdd(E mainEntity, Object subEntity) {}

  /**
   * Executed after a sub-entity is added to a main entity.
   *
   * @param mainEntity the main entity
   * @param subEntity  the added sub-entity
   */
  default void afterAdd(E mainEntity, Object subEntity) {}

  /**
   * Executed before an entity is updated.
   *
   * @param entity the entity to be updated
   */
  default void beforeUpdate(E entity) {}

  /**
   * Executed after an entity is updated.
   *
   * @param entity the updated entity
   */
  default void afterUpdate(E entity) {}

  /**
   * Executed before an entity is deleted.
   *
   * @param entity the entity to be deleted
   */
  default void beforeDelete(E entity) {}

  /**
   * Executed after an entity is deleted.
   *
   * @param entity the deleted entity
   */
  default void afterDelete(E entity) {}

  /**
   * Executed before a sub-entity is deleted from a main entity.
   *
   * @param mainEntity the main entity
   * @param subEntity  the sub-entity to be deleted
   */
  default void beforeDelete(E mainEntity, Object subEntity) {}

  /**
   * Executed after a sub-entity is deleted from a main entity.
   *
   * @param mainEntity the main entity
   * @param subEntity  the deleted sub-entity
   */
  default void afterDelete(E mainEntity, Object subEntity) {}
}
