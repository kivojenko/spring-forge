package com.kivojenko.spring.forge.example.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kivojenko.spring.forge.example.WithPostgres;
import com.kivojenko.spring.forge.example.model.Translation;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class TranslationControllerTest extends WithPostgres {

  @Autowired
  protected MockMvc mockMvc;

  protected final ObjectMapper objectMapper = new ObjectMapper();

  @BeforeEach
  void setup() throws Exception {
    cleanUp();
  }

  @AfterEach
  void cleanUp() throws Exception {
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
  }

  @Test
  void testBaseCount() throws Exception {
    mockMvc
        .perform(post("/translations")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Translation.builder().enUS("Hello").ruRU("Привет").build())))
        .andExpect(status().isCreated());

    mockMvc.perform(get("/translations/count")).andExpect(status().isOk()).andExpect(jsonPath("$", is(1)));
  }

  @Test
  void testBaseCreate() throws Exception {
    mockMvc
        .perform(post("/translations")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Translation.builder().enUS("World").ruRU("Мир").build())))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.enUS", is("World")))
        .andExpect(jsonPath("$.ruRU", is("Мир")))
        .andExpect(jsonPath("$.id").exists());
  }

  @Test
  void testBaseGetById() throws Exception {
    String json = mockMvc
        .perform(post("/translations")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Translation.builder().enUS("Get Me").build())))
        .andExpect(status().isCreated())
        .andReturn()
        .getResponse()
        .getContentAsString();

    Long id = objectMapper.readTree(json).get("id").asLong();

    mockMvc
        .perform(get("/translations/{id}", id))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.enUS", is("Get Me")));
  }

  @Test
  void testBaseUpdate() throws Exception {
    String json = mockMvc
        .perform(post("/translations")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Translation.builder().enUS("Old").build())))
        .andExpect(status().isCreated())
        .andReturn()
        .getResponse()
        .getContentAsString();

    Long id = objectMapper.readTree(json).get("id").asLong();

    mockMvc
        .perform(put("/translations/{id}", id)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Translation.builder().enUS("New").build())))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.enUS", is("New")));
  }

  @Test
  void testBaseDelete() throws Exception {
    String json = mockMvc
        .perform(post("/translations")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Translation.builder().enUS("Delete").build())))
        .andExpect(status().isCreated())
        .andReturn()
        .getResponse()
        .getContentAsString();

    Long id = objectMapper.readTree(json).get("id").asLong();

    mockMvc.perform(delete("/translations/{id}", id)).andExpect(status().isNoContent());

    assertThrows(ServletException.class, () -> mockMvc.perform(get("/translations/{id}", id)));
  }

  @Test
  void testGetTranslation() throws Exception {
    String json = mockMvc
        .perform(post("/translations")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Translation.builder().enUS("Hello").ruRU("Привет").build())))
        .andExpect(status().isCreated())
        .andReturn()
        .getResponse()
        .getContentAsString();

    Long id = objectMapper.readTree(json).get("id").asLong();

    mockMvc.perform(get("/translations/enUS/{id}", id)).andExpect(status().isOk()).andExpect(content().string("Hello"));

    mockMvc
        .perform(get("/translations/ruRU/{id}", id))
        .andExpect(status().isOk())
        .andExpect(content().string("Привет"));
  }
}
