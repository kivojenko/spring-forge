package com.kivojenko.spring.forge.example.aspect;

import com.kivojenko.spring.forge.example.model.hook.Item;
import com.kivojenko.spring.forge.example.model.hook.SubItem;
import com.kivojenko.spring.forge.jpa.contract.persistence.ForgePersistenceAspect;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Example implementation of {@link ForgePersistenceAspect} for {@link Item} entity.
 * This hook is automatically discovered and executed by {@code ForgePersistenceAspect}.
 */
@Slf4j
@Component
public class ItemPersistenceAspect extends ForgePersistenceAspect<Item> {
  @Override
  public void beforeCreate(Item item) {
    log.info("[HOOK] Before creating item: {}", item.getName());
    if (item.getPrice() != null && item.getPrice() < 0) {
      throw new IllegalArgumentException("Item price cannot be negative");
    }
  }

  @Override
  public void beforeUpdate(Item item) {
    log.info("[HOOK] Before updating item ID: {}", item.getId());

    if (item.getPrice() != null && item.getPrice() < 0) {
      throw new IllegalArgumentException("Item price cannot be negative");
    }
  }

  @Override
  public void beforeAdd(Object mainEntityId, Object subEntity) {
    if (subEntity instanceof SubItem subItem) {
      log.info("[HOOK] Before adding subitem: {} to item: {}", subItem.getName(), mainEntityId);
      if (subItem.getName() == null || subItem.getName().isBlank()) {
        throw new IllegalArgumentException("SubItem name cannot be empty");
      }
    }
  }
}
