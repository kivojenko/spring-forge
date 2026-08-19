package com.kivojenko.spring.forge.example.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.kivojenko.spring.forge.example.WithPostgres;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;

/**
 * Integration tests verifying filtering by dotted target fields (e.g. category.name)
 * and iterable relations (tags) for the generated Product endpoints.
 */
@SpringBootTest
@AutoConfigureMockMvc
public class ProductControllerFilterTest extends WithPostgres {

  private Long electronicsId;
  private Long booksId;
  private Long featuredTagId;
  private Long onSaleTagId;

  @BeforeEach
  void setUp() throws Exception {
    electronicsId = createCategory("Electronics");
    booksId = createCategory("Books");

    featuredTagId = createTag("Featured");
    onSaleTagId = createTag("OnSale");

    // Products
    createProduct(
        "Phone",
        "SKU-1",
        new BigDecimal("499.99"),
        electronicsId,
        new Long[] { featuredTagId }
    );

    createProduct(
        "Laptop",
        "SKU-2",
        new BigDecimal("1299.00"),
        electronicsId,
        new Long[] { featuredTagId, onSaleTagId }
    );

    createProduct(
        "Novel",
        "SKU-3",
        new BigDecimal("19.99"),
        booksId,
        new Long[] { onSaleTagId }
    );
  }

  private Long createCategory(String name) throws Exception {
    String body = "{" +
        "\"name\":\"" + name + "\"" +
        "}";

    String response = mockMvc.perform(post("/productCategories")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isCreated())
        .andReturn()
        .getResponse()
        .getContentAsString();

    JsonNode node = objectMapper.readTree(response);
    return node.get("id").asLong();
  }

  private Long createTag(String name) throws Exception {
    String body = "{" +
        "\"name\":\"" + name + "\"" +
        "}";

    String response = mockMvc.perform(post("/tags")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isCreated())
        .andReturn()
        .getResponse()
        .getContentAsString();

    JsonNode node = objectMapper.readTree(response);
    return node.get("id").asLong();
  }

  private void createProduct(String name, String sku, BigDecimal price, Long categoryId, Long[] tagIds)
      throws Exception {
    StringBuilder tagsJson = new StringBuilder("[");
    for (int i = 0; i < tagIds.length; i++) {
      if (i > 0) tagsJson.append(',');
      tagsJson.append("{\"id\":").append(tagIds[i]).append("}");
    }
    tagsJson.append(']');

    String body = "{" +
        "\"name\":\"" + name + "\"," +
        "\"sku\":\"" + sku + "\"," +
        "\"price\":" + price + "," +
        "\"active\":true," +
        "\"inStock\":true," +
        "\"category\":{\"id\":" + categoryId + "}," +
        "\"tags\":" + tagsJson +
        "}";

    mockMvc.perform(post("/products")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isCreated());
  }

  @Test
  void shouldFilterProductsByCategoryName_viaDottedTargetField() throws Exception {
    // Single category name
    mockMvc.perform(get("/products").param("categories", "Electronics"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(2)))
        .andExpect(jsonPath("$.content[?(@.name == 'Phone')]").exists())
        .andExpect(jsonPath("$.content[?(@.name == 'Laptop')]").exists());

    // Multiple category names
    mockMvc.perform(get("/products").param("categories", "Electronics,Books"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(3)));

    // Partial match, case-insensitive (CONTAINS_IGNORE_CASE configured on targetField)
    mockMvc.perform(get("/products").param("categories", "lectr"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(2)));

    // Non-existent category name
    mockMvc.perform(get("/products").param("categories", "Furniture"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(0)));
  }

  @Test
  void shouldFilterProductsByTags_iterableANY() throws Exception {
    // Filter by single tag (Featured)
    mockMvc.perform(get("/products").param("tags", String.valueOf(featuredTagId)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(2)))
        .andExpect(jsonPath("$.content[?(@.name == 'Phone')]").exists())
        .andExpect(jsonPath("$.content[?(@.name == 'Laptop')]").exists());

    // Filter by single tag (OnSale)
    mockMvc.perform(get("/products").param("tags", String.valueOf(onSaleTagId)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(2)))
        .andExpect(jsonPath("$.content[?(@.name == 'Laptop')]").exists())
        .andExpect(jsonPath("$.content[?(@.name == 'Novel')]").exists());

    // ANY mode: union of both tags should return all three products
    mockMvc.perform(get("/products").param("tags",
            String.valueOf(featuredTagId) + "," + String.valueOf(onSaleTagId)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(3)));
  }
}
