package com.kivojenko.spring.forge.example.controller;

import com.kivojenko.spring.forge.example.WithPostgres;
import com.kivojenko.spring.forge.example.model.filter.Product;
import com.kivojenko.spring.forge.example.model.filter.ProductForgeRepository;
import com.kivojenko.spring.forge.example.model.filter.ProductForgeService;
import com.kivojenko.spring.forge.example.model.filter.Tag;
import com.kivojenko.spring.forge.example.model.filter.TagForgeRepository;
import com.kivojenko.spring.forge.example.model.hook.Item;
import com.kivojenko.spring.forge.example.model.hook.ItemCategory;
import com.kivojenko.spring.forge.example.model.hook.ItemCategoryForgeRepository;
import com.kivojenko.spring.forge.example.model.hook.ItemForgeRepository;
import com.kivojenko.spring.forge.example.model.hook.ItemForgeService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
public class PatchCollectionTest extends WithPostgres {

  @Autowired
  private ProductForgeService productService;

  @Autowired
  private ProductForgeRepository productRepository;

  @Autowired
  private TagForgeRepository tagRepository;

  @Autowired
  private ItemForgeService itemService;

  @Autowired
  private ItemForgeRepository itemRepository;

  @Autowired
  private ItemCategoryForgeRepository itemCategoryRepository;

  @AfterEach
  void tearDown() {
    productRepository.deleteAll();
    tagRepository.deleteAll();
    itemRepository.deleteAll();
    itemCategoryRepository.deleteAll();
  }

  @Test
  void testPatchPassListWhenFieldIsSet() {
    Tag tag1 = tagRepository.save(Tag.builder().name("PatchTag1").build());
    Tag tag2 = tagRepository.save(Tag.builder().name("PatchTag2").build());

    Product product = productService.create(Product.builder()
        .name("Product With Tags")
        .sku("PATCH_SET_001")
        .tags(new HashSet<>())
        .build());

    // Field 'tags' is Set<Tag>. We pass a List<Tag> to patch
    List<Tag> tagList = List.of(tag1, tag2);
    Product patched = productService.patch(product.getId(), Map.of("tags", tagList));

    assertNotNull(patched.getTags());
    assertInstanceOf(Set.class, patched.getTags());
    assertEquals(2, patched.getTags().size());
    assertTrue(patched.getTags().stream().anyMatch(t -> t.getName().equals("PatchTag1")));
    assertTrue(patched.getTags().stream().anyMatch(t -> t.getName().equals("PatchTag2")));
  }

  @Test
  void testPatchPassSetWhenFieldIsList() {
    ItemCategory cat1 = itemCategoryRepository.save(ItemCategory.builder().name("Cat1").build());
    ItemCategory cat2 = itemCategoryRepository.save(ItemCategory.builder().name("Cat2").build());

    Item item = itemService.create(Item.builder()
        .name("Item With Categories")
        .price(25.0)
        .build());

    // Field 'categories' is List<ItemCategory>. We pass a Set<ItemCategory> to patch
    Set<ItemCategory> catSet = Set.of(cat1, cat2);
    Item patched = itemService.patch(item.getId(), Map.of("categories", catSet));

    assertNotNull(patched.getCategories());
    assertInstanceOf(List.class, patched.getCategories());
    assertEquals(2, patched.getCategories().size());
    assertTrue(patched.getCategories().stream().anyMatch(c -> c.getName().equals("Cat1")));
    assertTrue(patched.getCategories().stream().anyMatch(c -> c.getName().equals("Cat2")));
  }

  @Test
  void testConvertValueDirectUnitTests() throws Exception {
    java.lang.reflect.Method convertValueMethod = com.kivojenko.spring.forge.jpa.contract.ForgeService.class
        .getDeclaredMethod("convertValue", Object.class, Class.class);
    convertValueMethod.setAccessible(true);

    // List -> Set
    Object resSet = convertValueMethod.invoke(productService, List.of("x", "y"), Set.class);
    assertInstanceOf(Set.class, resSet);
    assertEquals(Set.of("x", "y"), resSet);

    // List -> SortedSet / TreeSet
    Object resTreeSet = convertValueMethod.invoke(productService, List.of("b", "a"), java.util.SortedSet.class);
    assertInstanceOf(java.util.TreeSet.class, resTreeSet);
    assertEquals(List.of("a", "b"), new java.util.ArrayList<>((java.util.SortedSet<?>) resTreeSet));

    // Set -> List / ArrayList
    Object resList = convertValueMethod.invoke(productService, Set.of("m", "n"), List.class);
    assertInstanceOf(List.class, resList);
    assertEquals(2, ((List<?>) resList).size());

    // Set -> LinkedList
    Object resLinkedList = convertValueMethod.invoke(productService, Set.of("m", "n"), java.util.LinkedList.class);
    assertInstanceOf(java.util.LinkedList.class, resLinkedList);
    assertEquals(2, ((java.util.LinkedList<?>) resLinkedList).size());

    // Array -> Set
    Object resArrayToSet = convertValueMethod.invoke(productService, new String[]{"1", "2"}, Set.class);
    assertInstanceOf(Set.class, resArrayToSet);
    assertEquals(Set.of("1", "2"), resArrayToSet);

    // Array -> List
    Object resArrayToList = convertValueMethod.invoke(productService, new String[]{"1", "2"}, List.class);
    assertInstanceOf(List.class, resArrayToList);
    assertEquals(List.of("1", "2"), resArrayToList);
  }
}
