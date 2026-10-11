package com.kivojenko.spring.forge.jpa.contract.persistence;

import java.util.List;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.data.domain.Page;

import com.kivojenko.spring.forge.jpa.contract.ForgeService;

/**
 * Interface/base class for read hooks that are automatically executed before and after read operations via AOP.
 * To use a hook, extend this class and register it as a Spring {@code @Bean} or {@code @Component}
 * for a specific entity type. The {@code ForgeReadAspect} will automatically
 * intercept read operations performed through {@link ForgeService}.
 *
 * @param <E> the entity type
 */
@Aspect
public abstract class ForgeReadAspect<E> {

  /**
   * Returns the entity class handled by this read aspect.
   *
   * @return the entity class
   */
  protected abstract Class<E> entityType();

  /**
   * Intercepts execution before {@link ForgeService#getById(Object)}.
   *
   * @param joinPoint the join point
   */
  @Before("within(com.kivojenko.spring.forge.jpa.contract.ForgeService+) && execution(* getById(..))")
  public void onBeforeGet(JoinPoint joinPoint) {
    if (joinPoint.getThis() instanceof ForgeService<?, ?, ?> service) {
      if (!service.getEntityClass().equals(entityType())) {
        return;
      }
      Object[] args = joinPoint.getArgs();
      if (args.length > 0) {
        beforeGet(args[0]);
      }
    }
  }

  /**
   * Hook executed before an entity is retrieved by its ID.
   *
   * @param id the ID of the entity to retrieve
   */
  public void beforeGet(Object id) {}

  /**
   * Intercepts successful return from {@link ForgeService#getById(Object)}.
   *
   * @param joinPoint the join point
   * @param entity the retrieved entity
   */
  @SuppressWarnings("unchecked")
  @AfterReturning(
      pointcut = "within(com.kivojenko.spring.forge.jpa.contract.ForgeService+) && execution(* getById(..))",
      returning = "entity"
  )
  public void onAfterGet(JoinPoint joinPoint, Object entity) {
    if (joinPoint.getThis() instanceof ForgeService<?, ?, ?> service) {
      if (!service.getEntityClass().equals(entityType()) || !entityType().isInstance(entity)) {
        return;
      }
      afterGet((E) entity);
    }
  }

  /**
   * Hook executed after an entity is successfully retrieved by its ID.
   *
   * @param entity the retrieved entity
   */
  public void afterGet(E entity) {}

  /**
   * Intercepts execution before {@code findAll} operations on {@link ForgeService}.
   *
   * @param joinPoint the join point
   */
  @Before("within(com.kivojenko.spring.forge.jpa.contract.ForgeService+) && execution(* findAll(..))")
  public void onBeforeFindAll(JoinPoint joinPoint) {
    if (joinPoint.getThis() instanceof ForgeService<?, ?, ?> service) {
      if (!service.getEntityClass().equals(entityType())) {
        return;
      }
      beforeFindAll();
    }
  }

  /**
   * Hook executed before finding all entities or executing a filtered/paged search.
   */
  public void beforeFindAll() {}

  /**
   * Intercepts successful return from {@code findAll} operations on {@link ForgeService}.
   *
   * @param joinPoint the join point
   * @param result the returned search result
   */
  @SuppressWarnings("unchecked")
  @AfterReturning(
      pointcut = "within(com.kivojenko.spring.forge.jpa.contract.ForgeService+) && execution(* findAll(..))",
      returning = "result"
  )
  public void onAfterFindAll(JoinPoint joinPoint, Object result) {
    if (joinPoint.getThis() instanceof ForgeService<?, ?, ?> service) {
      if (!service.getEntityClass().equals(entityType())) {
        return;
      }
      if (result instanceof Page<?> page) {
        afterFindAll((Page<E>) page);
      } else if (result instanceof List<?> list) {
        afterFindAll((List<E>) list);
      } else if (result instanceof Iterable<?> iterable) {
        afterFindAll((Iterable<E>) iterable);
      }
    }
  }

  /**
   * Hook executed after entities are retrieved as an {@link Iterable}.
   *
   * @param entities the retrieved entities
   */
  public void afterFindAll(Iterable<E> entities) {}

  /**
   * Hook executed after entities are retrieved as a {@link List}.
   * By default, forwards to {@link #afterFindAll(Iterable)}.
   *
   * @param entities the retrieved list of entities
   */
  public void afterFindAll(List<E> entities) {
    afterFindAll((Iterable<E>) entities);
  }

  /**
   * Hook executed after entities are retrieved as a {@link Page}.
   * By default, forwards to {@link #afterFindAll(Iterable)}.
   *
   * @param page the retrieved page of entities
   */
  public void afterFindAll(Page<E> page) {
    afterFindAll((Iterable<E>) page);
  }

  /**
   * Intercepts execution before {@link ForgeService#readAssociation(Object, String, Class)}.
   *
   * @param joinPoint the join point
   */
  @Before("within(com.kivojenko.spring.forge.jpa.contract.ForgeService+) && execution(* readAssociation(..))")
  public void onBeforeReadAssociation(JoinPoint joinPoint) {
    if (joinPoint.getThis() instanceof ForgeService<?, ?, ?> service) {
      if (!service.getEntityClass().equals(entityType())) {
        return;
      }
      Object[] args = joinPoint.getArgs();
      if (args.length >= 2) {
        beforeReadAssociation(args[0], (String) args[1]);
      }
    }
  }

  /**
   * Hook executed before an entity's association is read.
   *
   * @param id the owner entity ID
   * @param attribute the attribute name of the association
   */
  public void beforeReadAssociation(Object id, String attribute) {}

  /**
   * Intercepts successful return from {@link ForgeService#readAssociation(Object, String, Class)}.
   *
   * @param joinPoint the join point
   * @param result the returned list of associated entities
   */
  @SuppressWarnings("unchecked")
  @AfterReturning(
      pointcut = "within(com.kivojenko.spring.forge.jpa.contract.ForgeService+) && execution(* readAssociation(..))",
      returning = "result"
  )
  public void onAfterReadAssociation(JoinPoint joinPoint, Object result) {
    if (joinPoint.getThis() instanceof ForgeService<?, ?, ?> service) {
      if (!service.getEntityClass().equals(entityType())) {
        return;
      }
      Object[] args = joinPoint.getArgs();
      if (args.length >= 2 && result instanceof List<?> list) {
        afterReadAssociation(args[0], (String) args[1], list);
      }
    }
  }

  /**
   * Hook executed after an entity's association is read.
   *
   * @param id the owner entity ID
   * @param attribute the attribute name of the association
   * @param associatedEntities the retrieved associated entities
   */
  public void afterReadAssociation(Object id, String attribute, List<?> associatedEntities) {}

  /**
   * Intercepts execution before {@link ForgeService#count()}.
   *
   * @param joinPoint the join point
   */
  @Before("within(com.kivojenko.spring.forge.jpa.contract.ForgeService+) && execution(* count(..))")
  public void onBeforeCount(JoinPoint joinPoint) {
    if (joinPoint.getThis() instanceof ForgeService<?, ?, ?> service) {
      if (!service.getEntityClass().equals(entityType())) {
        return;
      }
      beforeCount();
    }
  }

  /**
   * Hook executed before counting entities.
   */
  public void beforeCount() {}

  /**
   * Intercepts successful return from {@link ForgeService#count()}.
   *
   * @param joinPoint the join point
   * @param count the total count of entities
   */
  @AfterReturning(
      pointcut = "within(com.kivojenko.spring.forge.jpa.contract.ForgeService+) && execution(* count(..))",
      returning = "count"
  )
  public void onAfterCount(JoinPoint joinPoint, Object count) {
    if (joinPoint.getThis() instanceof ForgeService<?, ?, ?> service) {
      if (!service.getEntityClass().equals(entityType())) {
        return;
      }
      if (count instanceof Long l) {
        afterCount(l);
      }
    }
  }

  /**
   * Hook executed after counting entities.
   *
   * @param count the total count
   */
  public void afterCount(long count) {}

  /**
   * Intercepts execution before {@link ForgeService#exists(Object)}.
   *
   * @param joinPoint the join point
   */
  @Before("within(com.kivojenko.spring.forge.jpa.contract.ForgeService+) && execution(* exists(..))")
  public void onBeforeExists(JoinPoint joinPoint) {
    if (joinPoint.getThis() instanceof ForgeService<?, ?, ?> service) {
      if (!service.getEntityClass().equals(entityType())) {
        return;
      }
      Object[] args = joinPoint.getArgs();
      if (args.length > 0) {
        beforeExists(args[0]);
      }
    }
  }

  /**
   * Hook executed before checking if an entity exists by ID.
   *
   * @param id the ID to check
   */
  public void beforeExists(Object id) {}

  /**
   * Intercepts successful return from {@link ForgeService#exists(Object)}.
   *
   * @param joinPoint the join point
   * @param exists the result of the existence check
   */
  @AfterReturning(
      pointcut = "within(com.kivojenko.spring.forge.jpa.contract.ForgeService+) && execution(* exists(..))",
      returning = "exists"
  )
  public void onAfterExists(JoinPoint joinPoint, Object exists) {
    if (joinPoint.getThis() instanceof ForgeService<?, ?, ?> service) {
      if (!service.getEntityClass().equals(entityType())) {
        return;
      }
      Object[] args = joinPoint.getArgs();
      if (args.length > 0 && exists instanceof Boolean b) {
        afterExists(args[0], b);
      }
    }
  }

  /**
   * Hook executed after checking if an entity exists by ID.
   *
   * @param id the ID that was checked
   * @param exists whether the entity exists
   */
  public void afterExists(Object id, boolean exists) {}
}
