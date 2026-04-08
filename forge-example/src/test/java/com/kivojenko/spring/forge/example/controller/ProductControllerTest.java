package com.kivojenko.spring.forge.example.controller;

import com.kivojenko.spring.forge.example.WithPostgres;
import com.kivojenko.spring.forge.example.model.filter.Product;
import com.kivojenko.spring.forge.example.model.filter.ProductCategory;
import com.kivojenko.spring.forge.example.model.filter.Tag;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ProductControllerTest extends WithPostgres {

  private Long category1Id;
  private Long category2Id;
  private Long tag1Id;
  private Long tag2Id;

  @BeforeEach
  void setUp() throws Exception {
    category1Id = createCategory("Electronics");
    category2Id = createCategory("Books");
    tag1Id = createTag("Sale");
    tag2Id = createTag("New");

    createProduct("Laptop", "SKU001", new BigDecimal("999.99"), true, "Apple", true, 1.2, category1Id, Set.of(tag1Id), LocalDateTime.now());
    createProduct("Smartphone",
                  "SKU002",
                  new BigDecimal("599.99"),
                  true,
                  "Samsung",
                  true,
                  0.2,
                  category1Id,
                  Set.of(tag1Id, tag2Id),
                  LocalDateTime.now().minusDays(5));
    createProduct("Java Book",
                  "SKU003",
                  new BigDecimal("49.99"),
                  false,
                  "O'Reilly",
                  false,
                  0.8,
                  category2Id,
                  Set.of(tag2Id),
                  LocalDateTime.now().minusDays(10));
  }

  private Long createCategory(String name) throws Exception {
    String json = mockMvc.perform(post("/productCategories").contentType(MediaType.APPLICATION_JSON)
                                      .content(objectMapper.writeValueAsString(ProductCategory.builder()
                                                                                   .name(name)
                                                                                   .build())))
        .andExpect(status().isCreated())
        .andReturn()
        .getResponse()
        .getContentAsString();
    return objectMapper.readTree(json).get("id").asLong();
  }

  private Long createTag(String name) throws Exception {
    String json = mockMvc.perform(post("/tags").contentType(MediaType.APPLICATION_JSON)
                                      .content(objectMapper.writeValueAsString(Tag.builder().name(name).build())))
        .andExpect(status().isCreated())
        .andReturn()
        .getResponse()
        .getContentAsString();
    return objectMapper.readTree(json).get("id").asLong();
  }

  private void createProduct(String name,
                             String sku,
                             BigDecimal price,
                             boolean active,
                             String brand,
                             boolean inStock,
                             Double weight,
                             Long categoryId,
                             Set<Long> tagIds,
                             LocalDateTime createdAt) throws Exception {
    Product.ProductBuilder builder = Product.builder()
        .name(name)
        .sku(sku)
        .price(price)
        .active(active)
        .brand(brand)
        .inStock(inStock)
        .weight(weight)
        .category(ProductCategory.builder().id(categoryId).build())
        .createdAt(createdAt);

    Product product = builder.build();
    tagIds.forEach(id -> product.getTags().add(Tag.builder().id(id).build()));

    mockMvc.perform(post("/products").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(product))).andExpect(status().isCreated());
  }

  @Test
  void testFilterByNameContains() throws Exception {
    mockMvc.perform(get("/products").param("name", "phone"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(1)))
        .andExpect(jsonPath("$.content[0].name", is("Smartphone")));
  }

  @Test
  void testFilterBySkuExact() throws Exception {
    mockMvc.perform(get("/products").param("sku", "SKU002"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(1)))
        .andExpect(jsonPath("$.content[0].sku", is("SKU002")));

    mockMvc.perform(get("/products").param("sku", "SKU00"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(0)));
  }

  @Test
  void testFilterByPriceRange() throws Exception {
    // minPrice only
    mockMvc.perform(get("/products").param("minPrice", "500"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(2))); // Laptop (999.99) and Smartphone (599.99)

    // maxPrice only
    mockMvc.perform(get("/products").param("maxPrice", "100"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(1)))
        .andExpect(jsonPath("$.content[0].name", is("Java Book"))); // Java Book (49.99)

    // both minPrice and maxPrice
    mockMvc.perform(get("/products").param("minPrice", "50").param("maxPrice", "700"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(1)))
        .andExpect(jsonPath("$.content[0].name", is("Smartphone"))); // Smartphone (599.99)
  }

  @Test
  void testFilterByActive() throws Exception {
    mockMvc.perform(get("/products").param("active", "true"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(2)));

    mockMvc.perform(get("/products").param("active", "false"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(1)))
        .andExpect(jsonPath("$.content[0].name", is("Java Book")));
  }

  @Test
  void testFilterByCategory() throws Exception {
    mockMvc.perform(get("/products").param("categories", category1Id.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(2)));
  }

  @Test
  void testFilterByTagsAny() throws Exception {
    mockMvc.perform(get("/products").param("tags", tag2Id.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(2))); // Smartphone and Java Book

    mockMvc.perform(get("/products").param("tags", tag1Id + "," + tag2Id))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(3))); // All products have at least one of these tags
  }

  @Test
  void testFilterByBrand() throws Exception {
    mockMvc.perform(get("/products").param("brand", "Samsung"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(1)))
        .andExpect(jsonPath("$.content[0].brand", is("Samsung")));
  }

  @Test
  void testFilterByInStock() throws Exception {
    mockMvc.perform(get("/products").param("inStock", "true"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(2)));

    mockMvc.perform(get("/products").param("inStock", "false"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(1)))
        .andExpect(jsonPath("$.content[0].name", is("Java Book")));
  }

  @Test
  void testFilterByWeight() throws Exception {
    mockMvc.perform(get("/products").param("weight", "0.8"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(1)))
        .andExpect(jsonPath("$.content[0].name", is("Java Book")));
  }

  @Test
  void testFilterByCreatedAtRange() throws Exception {
    LocalDateTime now = LocalDateTime.now();
    String minDate = now.minusDays(7).toString();
    String maxDate = now.minusDays(3).toString();

    // minCreatedAt only
    mockMvc.perform(get("/products").param("minCreatedAt", minDate))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(2))); // Laptop (now) and Smartphone (now-5d)

    // maxCreatedAt only
    mockMvc.perform(get("/products").param("maxCreatedAt", maxDate))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(2))); // Smartphone (now-5d) and Java Book (now-10d)

    // both
    mockMvc.perform(get("/products").param("minCreatedAt", minDate).param("maxCreatedAt", maxDate))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(1)))
        .andExpect(jsonPath("$.content[0].name", is("Smartphone")));
  }

  @Test
  void testCombinedFilters() throws Exception {
    mockMvc.perform(get("/products").param("categories", category1Id.toString())
                        .param("active", "true")
                        .param("name", "Lap"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(1)))
        .andExpect(jsonPath("$.content[0].name", is("Laptop")));
  }

  @Test
  void testSortByPriceAsc() throws Exception {
    mockMvc.perform(get("/products").param("sort", "price,asc"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(3)))
        .andExpect(jsonPath("$.content[0].name", is("Java Book"))) // 49.99
        .andExpect(jsonPath("$.content[1].name", is("Smartphone"))) // 599.99
        .andExpect(jsonPath("$.content[2].name", is("Laptop"))); // 999.99
  }

  @Test
  void testSortByPriceDesc() throws Exception {
    mockMvc.perform(get("/products").param("sort", "price,desc"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(3)))
        .andExpect(jsonPath("$.content[0].name", is("Laptop")))
        .andExpect(jsonPath("$.content[1].name", is("Smartphone")))
        .andExpect(jsonPath("$.content[2].name", is("Java Book")));
  }

  @Test
  void testSortByName() throws Exception {
    mockMvc.perform(get("/products").param("sort", "name,asc"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(3)))
        .andExpect(jsonPath("$.content[0].name", is("Java Book")))
        .andExpect(jsonPath("$.content[1].name", is("Laptop")))
        .andExpect(jsonPath("$.content[2].name", is("Smartphone")));
  }
}
