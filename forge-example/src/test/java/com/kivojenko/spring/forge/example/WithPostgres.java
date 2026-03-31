package com.kivojenko.spring.forge.example;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Testcontainers
@Import(TestPostgresConfig.class)
public abstract class WithPostgres {
  @Autowired
  protected MockMvc mockMvc;

  protected final ObjectMapper objectMapper = new ObjectMapper();


  @AfterEach
  public void cleanUp() throws Exception {
    String productsJson = mockMvc
        .perform(get("/products"))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();

    JsonNode productsRoot = objectMapper.readTree(productsJson);
    for (JsonNode product : productsRoot.get("content")) {
      Long productId = product.get("id").asLong();
      mockMvc.perform(delete("/products/{id}", productId)).andExpect(status().isNoContent());
    }

    String productCategoriesJson = mockMvc
        .perform(get("/productCategories"))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();

    JsonNode productCategoriesRoot = objectMapper.readTree(productCategoriesJson);
    for (JsonNode category : productCategoriesRoot.get("content")) {
      Long categoryId = category.get("id").asLong();
      mockMvc.perform(delete("/productCategories/{id}", categoryId)).andExpect(status().isNoContent());
    }

    String tagsJson = mockMvc
        .perform(get("/tags"))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();

    JsonNode tagsRoot = objectMapper.readTree(tagsJson);
    for (JsonNode tag : tagsRoot.get("content")) {
      Long tagId = tag.get("id").asLong();
      mockMvc.perform(delete("/tags/{id}", tagId)).andExpect(status().isNoContent());
    }

    String booksJson = mockMvc
        .perform(get("/books"))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();

    JsonNode booksRoot = objectMapper.readTree(booksJson);
    for (JsonNode book : booksRoot.get("content")) {
      Long bookId = book.get("id").asLong();
      mockMvc.perform(delete("/books/{id}", bookId)).andExpect(status().isNoContent());
    }

    String authorsJson = mockMvc
        .perform(get("/authors"))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();

    JsonNode authorsRoot = objectMapper.readTree(authorsJson);
    for (JsonNode author : authorsRoot.get("content")) {
      Long authorId = author.get("id").asLong();
      mockMvc.perform(delete("/authors/{id}", authorId)).andExpect(status().isNoContent());
    }

    String categoriesJson = mockMvc
        .perform(get("/categories"))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();

    JsonNode categoriesRoot = objectMapper.readTree(categoriesJson);
    for (JsonNode category : categoriesRoot.get("content")) {
      Long categoryId = category.get("id").asLong();
      mockMvc.perform(delete("/categories/{id}", categoryId)).andExpect(status().isNoContent());
    }

    String translationsJson = mockMvc
        .perform(get("/translations"))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();

    JsonNode translationsRoot = objectMapper.readTree(translationsJson);
    for (JsonNode translation : translationsRoot.get("content")) {
      Long translationId = translation.get("id").asLong();
      mockMvc.perform(delete("/translations/{id}", translationId)).andExpect(status().isNoContent());
    }
  }

}
