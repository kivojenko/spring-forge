package com.kivojenko.spring.forge.example.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.kivojenko.spring.forge.example.WithPostgres;
import com.kivojenko.spring.forge.example.model.filter.ProductCategoryForgeRepository;
import com.kivojenko.spring.forge.example.model.filter.ProductForgeRepository;
import com.kivojenko.spring.forge.example.model.filter.TagForgeRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
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

  @Autowired
  private ProductForgeRepository productRepository;

  @Autowired
  private ProductCategoryForgeRepository categoryRepository;

  @Autowired
  private TagForgeRepository tagRepository;

  private Long electronicsId;
  private Long booksId;
  private Long featuredTagId;
  private Long onSaleTagId;

  @BeforeEach
  void setUp() throws Exception {
    productRepository.deleteAll();
    categoryRepository.deleteAll();
    tagRepository.deleteAll();

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

  @AfterEach
  void tearDown() {
    productRepository.deleteAll();
    categoryRepository.deleteAll();
    tagRepository.deleteAll();
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
    mockMvc.perform(get("/products").param("category", "Electronics"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(2)))
        .andExpect(jsonPath("$.content[?(@.name == 'Phone')]").exists())
        .andExpect(jsonPath("$.content[?(@.name == 'Laptop')]").exists());

    // Partial match, case-insensitive (CONTAINS_IGNORE_CASE configured on targetField)
    mockMvc.perform(get("/products").param("category", "lectr"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(2)));

    // Non-existent category name
    mockMvc.perform(get("/products").param("category", "Furniture"))
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

  @Test
  void shouldFilterProductsByPresence_viaIsPresent() throws Exception {
    mockMvc.perform(post("/products")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\":\"Tablet\",\"sku\":\"SKU-4\",\"description\":\"Big screen\",\"inStock\":true,"
                + "\"category\":{\"id\":" + electronicsId + "},\"tags\":[]}"))
        .andExpect(status().isCreated());

    mockMvc.perform(get("/products").param("hasDescription", "true"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(1)))
        .andExpect(jsonPath("$.content[0].name").value("Tablet"));

    mockMvc.perform(get("/products").param("hasDescription", "false"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(3)));

    mockMvc.perform(get("/products").param("hasTags", "false"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(1)))
        .andExpect(jsonPath("$.content[0].name").value("Tablet"));

    mockMvc.perform(get("/products").param("hasTags", "true"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(3)));
  }

  @Test
  void shouldFilterProductsByEmbeddedTargetField_valueAndDefaultNamedPresence() throws Exception {
    mockMvc.perform(post("/products")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\":\"Paint\",\"sku\":\"SKU-5\",\"inStock\":true,\"color\":{\"colorIndex\":77000},"
                + "\"category\":{\"id\":" + booksId + "},\"tags\":[]}"))
        .andExpect(status().isCreated());

    // unnamed isPresent filter defaults to has<Field> and targets color.colorIndex
    mockMvc.perform(get("/products").param("hasColor", "true"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(1)))
        .andExpect(jsonPath("$.content[0].name").value("Paint"));

    mockMvc.perform(get("/products").param("hasColor", "false"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(3)));

    // value filter on the same embedded field keeps the field name and the target's type
    mockMvc.perform(get("/products").param("color", "77000"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(1)))
        .andExpect(jsonPath("$.content[0].name").value("Paint"));

    mockMvc.perform(get("/products").param("minColor", "80000"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(0)));
  }
}
