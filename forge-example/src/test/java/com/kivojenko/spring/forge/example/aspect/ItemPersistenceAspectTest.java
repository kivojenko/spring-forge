package com.kivojenko.spring.forge.example.aspect;

import com.kivojenko.spring.forge.example.WithPostgres;
import com.kivojenko.spring.forge.example.model.hook.Item;
import com.kivojenko.spring.forge.example.model.hook.ItemForgeService;
import com.kivojenko.spring.forge.example.model.hook.SubItem;
import com.kivojenko.spring.forge.example.model.hook.SubItemForgeRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
public class ItemPersistenceAspectTest extends WithPostgres {

  @Autowired
  private ItemForgeService itemService;

  @Autowired
  private SubItemForgeRepository subItemForgeRepository;

  @Test
  void testHookValidationOnCreate() {
    Item item = Item.builder().name("Invalid Item").price(-10.0).build();

    RuntimeException exception = assertThrows(RuntimeException.class, () -> itemService.create(item));
    assertEquals("Item price cannot be negative", exception.getMessage());
  }

  @Test
  void testHookValidationOnUpdate() {
    Item item = Item.builder().name("Valid Item").price(10.0).build();
    item = itemService.create(item);

    item.setPrice(-5.0);
    Item finalItem = item;
    RuntimeException exception = assertThrows(RuntimeException.class, () -> itemService.update(finalItem.getId(), finalItem));
    assertEquals("Item price cannot be negative", exception.getMessage());
  }

  @Test
  void testAddSubItemWithHookValidation() {
    Item item = Item.builder().name("Item for subitem").price(10.0).build();
    item = itemService.create(item);

    SubItem finalSubItem = SubItem.builder().name("").build();
    Item finalItem = item;
    RuntimeException exception = assertThrows(RuntimeException.class, () -> itemService.addNewSubItem(finalItem.getId(), finalSubItem));
    assertEquals("SubItem name cannot be empty", exception.getMessage());
  }

  @Test
  void testAddAndRemoveSubItem() {
    Item item = Item.builder().name("Item for subitem").price(10.0).build();
    item = itemService.create(item);

    SubItem subItem = SubItem.builder().name("Valid SubItem").build();
    subItem = itemService.addNewSubItem(item.getId(), subItem);

    List<SubItem> subItems = subItemForgeRepository.findAll();
    assertEquals(1, subItems.size());
    assertEquals("Valid SubItem", subItems.get(0).getName());
    assertEquals(item.getId(), subItems.get(0).getItem().getId());

    itemService.removeSubItem(item.getId(), subItem.getId());
    subItems = subItemForgeRepository.findAll();
    assertEquals(0, subItems.size());
  }
}
