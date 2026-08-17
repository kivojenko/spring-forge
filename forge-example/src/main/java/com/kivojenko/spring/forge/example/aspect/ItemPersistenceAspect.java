package com.kivojenko.spring.forge.example.aspect;

import com.kivojenko.spring.forge.example.model.hook.Item;
import com.kivojenko.spring.forge.example.model.hook.SubItem;
import com.kivojenko.spring.forge.jpa.contract.persistence.ForgePersistenceAspect;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Example implementation of {@link ForgePersistenceAspect} for {@link Item} entity.
 * This hook is automatically discovered and executed by {@code ForgePersistenceAspect}.
 *
 * <p>This example overrides every available hook to show what a full audit trail for
 * an entity (including its associations) looks like. Real auditors don't need to
 * override every method — only the ones they care about; every hook defaults to a no-op.
 */
@Slf4j
@Component
public class ItemPersistenceAspect extends ForgePersistenceAspect<Item> {
  @Override
  protected Class<Item> entityType() {
    return Item.class;
  }

  @Override
  public void beforeCreate(Item item) {
    log.info("[AUDIT] Creating item: {}", item.getName());
    if (item.getPrice() != null && item.getPrice() < 0) {
      throw new IllegalArgumentException("Item price cannot be negative");
    }
  }

  @Override
  public void afterCreate(Item item) {
    log.info("[AUDIT] Created item id={} name={}", item.getId(), item.getName());
  }

  @Override
  public void beforeUpdate(Item item) {
    log.info("[AUDIT] Updating item id={}", item.getId());

    if (item.getPrice() != null && item.getPrice() < 0) {
      throw new IllegalArgumentException("Item price cannot be negative");
    }
  }

  @Override
  public void afterUpdate(Item item) {
    // Also invoked for PATCH (partial update) requests, not just PUT.
    log.info("[AUDIT] Updated item id={} name={} price={}", item.getId(), item.getName(), item.getPrice());
  }

  @Override
  public void beforeDelete(Item item) {
    log.info("[AUDIT] Deleting item id={} name={}", item.getId(), item.getName());
  }

  @Override
  public void afterDelete(Item item) {
    log.info("[AUDIT] Deleted item id={} name={}", item.getId(), item.getName());
  }

  @Override
  public void beforeAdd(Object mainEntityId, Object subEntity) {
    if (subEntity instanceof SubItem subItem) {
      log.info("[AUDIT] Adding subitem: {} to item: {}", subItem.getName(), mainEntityId);
      if (subItem.getName() == null || subItem.getName().isBlank()) {
        throw new IllegalArgumentException("SubItem name cannot be empty");
      }
    }
  }

  @Override
  public void afterAdd(Object mainEntityId, Object subEntity) {
    if (subEntity instanceof SubItem subItem) {
      log.info("[AUDIT] Added subitem: {} to item: {}", subItem.getName(), mainEntityId);
    }
  }

  @Override
  public void beforeDelete(Object mainEntityId, Object subEntity) {
    log.info("[AUDIT] Removing subitem/association {} from item: {}", subEntity, mainEntityId);
  }

  @Override
  public void afterDelete(Object mainEntityId, Object subEntity) {
    log.info("[AUDIT] Removed subitem/association {} from item: {}", subEntity, mainEntityId);
  }
}
