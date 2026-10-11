package com.kivojenko.spring.forge.example.aspect;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;

import com.kivojenko.spring.forge.example.WithPostgres;
import com.kivojenko.spring.forge.example.model.general.Author;
import com.kivojenko.spring.forge.example.model.general.AuthorForgeService;
import com.kivojenko.spring.forge.example.model.hook.Item;
import com.kivojenko.spring.forge.example.model.hook.ItemCategory;
import com.kivojenko.spring.forge.example.model.hook.ItemForgeService;
import com.kivojenko.spring.forge.example.model.hook.SubItem;
import com.kivojenko.spring.forge.example.model.view.Views;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
public class ItemReadAspectTest extends WithPostgres {

  @Autowired
  private ItemForgeService itemService;

  @Autowired
  private AuthorForgeService authorService;

  @Autowired
  private ItemReadAspect itemReadAspect;

  @BeforeEach
  void setUp() {
    itemReadAspect.reset();
  }

  @Test
  void testGetByIdTriggersReadHooks() {
    Item item = itemService.create(Item.builder().name("Test Item").price(20.0).build());
    itemReadAspect.reset();

    Item found = itemService.getById(item.getId());
    assertNotNull(found);
    assertEquals("Test Item", found.getName());

    assertEquals(1, itemReadAspect.getBeforeGetCount().get());
    assertEquals(1, itemReadAspect.getAfterGetCount().get());
  }

  @Test
  void testFindAllListTriggersReadHooks() {
    itemService.create(Item.builder().name("Item 1").price(10.0).build());
    itemReadAspect.reset();

    List<Item> all = itemService.findAll();
    assertNotNull(all);
    assertTrue(all.size() >= 1);

    assertEquals(1, itemReadAspect.getBeforeFindAllCount().get());
    assertEquals(1, itemReadAspect.getAfterFindAllCount().get());
  }

  @Test
  void testFindAllPageableTriggersReadHooks() {
    itemService.create(Item.builder().name("Item Pageable").price(15.0).build());
    itemReadAspect.reset();

    var page = itemService.findAll(PageRequest.of(0, 10));
    assertNotNull(page);
    assertTrue(page.getTotalElements() >= 1);

    assertEquals(1, itemReadAspect.getBeforeFindAllCount().get());
    assertEquals(1, itemReadAspect.getAfterFindAllCount().get());
  }

  @Test
  void testCountAndExistsTriggerReadHooks() {
    Item item = itemService.create(Item.builder().name("Count/Exists Item").price(30.0).build());
    itemReadAspect.reset();

    long count = itemService.count();
    assertTrue(count >= 1);
    assertEquals(1, itemReadAspect.getBeforeCountCalls().get());
    assertEquals(1, itemReadAspect.getAfterCountCalls().get());

    boolean exists = itemService.exists(item.getId());
    assertTrue(exists);
    assertEquals(1, itemReadAspect.getBeforeExistsCalls().get());
    assertEquals(1, itemReadAspect.getAfterExistsCalls().get());
  }

  @Test
  void testReadAssociationTriggersReadHooks() {
    Item item = itemService.create(Item.builder().name("Association Item").price(25.0).build());
    SubItem subItem = itemService.addNewSubItem(item.getId(), SubItem.builder().name("Sub 1").build());
    assertNotNull(subItem);
    itemReadAspect.reset();

    List<SubItem> subItems = itemService.readAssociation(item.getId(), "subItems", Views.Summary.class);
    assertNotNull(subItems);
    assertEquals(1, subItems.size());

    assertEquals(1, itemReadAspect.getBeforeReadAssociationCount().get());
    assertEquals(1, itemReadAspect.getAfterReadAssociationCount().get());
  }

  @Test
  void testEntityTypeIsolation() {
    Author author = authorService.create(Author.builder().name("Test Author").build());
    itemReadAspect.reset();

    Author found = authorService.getById(author.getId());
    assertNotNull(found);
    authorService.findAll();
    authorService.count();
    authorService.exists(author.getId());

    assertEquals(0, itemReadAspect.getBeforeGetCount().get());
    assertEquals(0, itemReadAspect.getAfterGetCount().get());
    assertEquals(0, itemReadAspect.getBeforeFindAllCount().get());
    assertEquals(0, itemReadAspect.getAfterFindAllCount().get());
    assertEquals(0, itemReadAspect.getBeforeCountCalls().get());
    assertEquals(0, itemReadAspect.getAfterCountCalls().get());
    assertEquals(0, itemReadAspect.getBeforeExistsCalls().get());
    assertEquals(0, itemReadAspect.getAfterExistsCalls().get());
  }
}
