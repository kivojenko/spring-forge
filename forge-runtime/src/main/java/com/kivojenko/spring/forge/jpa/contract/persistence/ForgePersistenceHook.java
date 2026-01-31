package com.kivojenko.spring.forge.jpa.contract.persistence;

public interface ForgePersistenceHook<E> {

  default void beforeCreate(E entity) {}

  default void afterCreate(E entity) {}

  default void beforeAdd(E mainEntity, Object subEntity) {}

  default void afterAdd(E mainEntity, Object subEntity) {}

  default void beforeUpdate(E entity) {}

  default void afterUpdate(E entity) {}

  default void beforeDelete(E entity) {}

  default void afterDelete(E entity) {}

  default void beforeDelete(E mainEntity, Object subEntity) {}

  default void afterDelete(E mainEntity, Object subEntity) {}
}
