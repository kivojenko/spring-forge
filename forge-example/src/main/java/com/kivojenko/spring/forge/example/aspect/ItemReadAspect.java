package com.kivojenko.spring.forge.example.aspect;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import com.kivojenko.spring.forge.example.model.hook.Item;
import com.kivojenko.spring.forge.jpa.contract.persistence.ForgeReadAspect;

/**
 * Example implementation of {@link ForgeReadAspect} for {@link Item} entity.
 * This hook is automatically discovered and executed by {@code ForgeReadAspect}.
 */
@Slf4j
@Getter
@Component
public class ItemReadAspect extends ForgeReadAspect<Item> {

  private final AtomicInteger beforeGetCount = new AtomicInteger();
  private final AtomicInteger afterGetCount = new AtomicInteger();
  private final AtomicInteger beforeFindAllCount = new AtomicInteger();
  private final AtomicInteger afterFindAllCount = new AtomicInteger();
  private final AtomicInteger beforeReadAssociationCount = new AtomicInteger();
  private final AtomicInteger afterReadAssociationCount = new AtomicInteger();
  private final AtomicInteger beforeCountCalls = new AtomicInteger();
  private final AtomicInteger afterCountCalls = new AtomicInteger();
  private final AtomicInteger beforeExistsCalls = new AtomicInteger();
  private final AtomicInteger afterExistsCalls = new AtomicInteger();

  public void reset() {
    beforeGetCount.set(0);
    afterGetCount.set(0);
    beforeFindAllCount.set(0);
    afterFindAllCount.set(0);
    beforeReadAssociationCount.set(0);
    afterReadAssociationCount.set(0);
    beforeCountCalls.set(0);
    afterCountCalls.set(0);
    beforeExistsCalls.set(0);
    afterExistsCalls.set(0);
  }

  @Override
  protected Class<Item> entityType() {
    return Item.class;
  }

  @Override
  public void beforeGet(Object id) {
    log.info("[AUDIT-READ] Getting item with id: {}", id);
    beforeGetCount.incrementAndGet();
  }

  @Override
  public void afterGet(Item item) {
    log.info("[AUDIT-READ] Retrieved item: id={}, name={}", item.getId(), item.getName());
    afterGetCount.incrementAndGet();
  }

  @Override
  public void beforeFindAll() {
    log.info("[AUDIT-READ] Finding all items");
    beforeFindAllCount.incrementAndGet();
  }

  @Override
  public void afterFindAll(List<Item> items) {
    log.info("[AUDIT-READ] Found {} items via List", items.size());
    afterFindAllCount.incrementAndGet();
  }

  @Override
  public void afterFindAll(Page<Item> page) {
    log.info("[AUDIT-READ] Found {} items on page {} of {}", page.getNumberOfElements(), page.getNumber(), page.getTotalPages());
    afterFindAllCount.incrementAndGet();
  }

  @Override
  public void beforeReadAssociation(Object id, String attribute) {
    log.info("[AUDIT-READ] Reading association {} for item id: {}", attribute, id);
    beforeReadAssociationCount.incrementAndGet();
  }

  @Override
  public void afterReadAssociation(Object id, String attribute, List<?> associatedEntities) {
    log.info("[AUDIT-READ] Read association {} for item id: {}, found {} entries", attribute, id, associatedEntities.size());
    afterReadAssociationCount.incrementAndGet();
  }

  @Override
  public void beforeCount() {
    log.info("[AUDIT-READ] Counting items");
    beforeCountCalls.incrementAndGet();
  }

  @Override
  public void afterCount(long count) {
    log.info("[AUDIT-READ] Counted items: {}", count);
    afterCountCalls.incrementAndGet();
  }

  @Override
  public void beforeExists(Object id) {
    log.info("[AUDIT-READ] Checking existence of item id: {}", id);
    beforeExistsCalls.incrementAndGet();
  }

  @Override
  public void afterExists(Object id, boolean exists) {
    log.info("[AUDIT-READ] Checked existence of item id: {}, exists={}", id, exists);
    afterExistsCalls.incrementAndGet();
  }
}
