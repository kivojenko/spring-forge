package com.kivojenko.spring.forge.example.controller;

import com.kivojenko.spring.forge.example.WithPostgres;
import com.kivojenko.spring.forge.example.model.Category;
import com.kivojenko.spring.forge.example.model.Translation;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class CategoryControllerTest extends WithPostgres {

  @Test
  void testBaseCount() throws Exception {
    mockMvc
        .perform(post("/categories")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Category.builder().name("Category 1").build())))
        .andExpect(status().isCreated());

    mockMvc.perform(get("/categories/count")).andExpect(status().isOk()).andExpect(jsonPath("$", is(1)));
  }

  @Test
  void testBaseCreate() throws Exception {
    mockMvc
        .perform(post("/categories")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Category.builder().name("New Category").build())))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.name", is("New Category")))
        .andExpect(jsonPath("$.id").exists());
  }

  @Test
  void testBaseGetById() throws Exception {
    String json = mockMvc
        .perform(post("/categories")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Category.builder().name("Get Me").build())))
        .andExpect(status().isCreated())
        .andReturn()
        .getResponse()
        .getContentAsString();

    Long id = objectMapper.readTree(json).get("id").asLong();

    mockMvc.perform(get("/categories/{id}", id)).andExpect(status().isOk()).andExpect(jsonPath("$.name", is("Get Me")));
  }

  @Test
  void testBaseUpdate() throws Exception {
    String json = mockMvc
        .perform(post("/categories")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Category.builder().name("Old Name").build())))
        .andExpect(status().isCreated())
        .andReturn()
        .getResponse()
        .getContentAsString();

    Long id = objectMapper.readTree(json).get("id").asLong();

    mockMvc
        .perform(put("/categories/{id}", id)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Category.builder().name("New Name").build())))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.name", is("New Name")));
  }

  @Test
  void testBaseDelete() throws Exception {
    String json = mockMvc
        .perform(post("/categories")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Category.builder().name("Delete Me").build())))
        .andExpect(status().isCreated())
        .andReturn()
        .getResponse()
        .getContentAsString();

    Long id = objectMapper.readTree(json).get("id").asLong();

    mockMvc.perform(delete("/categories/{id}", id)).andExpect(status().isNoContent());

    assertThrows(ServletException.class, () -> mockMvc.perform(get("/categories/{id}", id)));
  }

  @Test
  void testGetOrCreate() throws Exception {
    // The first call creates the category
    mockMvc
        .perform(post("/categories/get-or-create").param("name", "Unique Category"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name", is("Unique Category")))
        .andExpect(jsonPath("$.id").exists());

    mockMvc.perform(get("/categories/count")).andExpect(status().isOk()).andExpect(jsonPath("$", is(1)));

    // The second call returns the same category
    mockMvc
        .perform(post("/categories/get-or-create").param("name", "Unique Category"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name", is("Unique Category")))
        .andExpect(jsonPath("$.id").exists());

    mockMvc.perform(get("/categories/count")).andExpect(status().isOk()).andExpect(jsonPath("$", is(1)));
  }

  @Test
  void testGetNameTranslation() throws Exception {

    String categoryJson = mockMvc
        .perform(post("/categories")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Category
                .builder()
                .name("Translated Category")
                .nameTranslation(Translation.builder().enUS("Category Name").build())
                .build())))
        .andExpect(status().isCreated())
        .andReturn()
        .getResponse()
        .getContentAsString();

    Long categoryId = objectMapper.readTree(categoryJson).get("id").asLong();

    mockMvc
        .perform(get("/categories/{id}/nameTranslation", categoryId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.enUS", is("Category Name")));
  }

  @Test
  void testRemoveNameTranslation() throws Exception {
    String categoryJson = mockMvc
        .perform(post("/categories")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Category
                .builder()
                .name("Category with Translation")
                .nameTranslation(Translation.builder().enUS("Category Name").build())
                .build())))
        .andExpect(status().isCreated())
        .andReturn()
        .getResponse()
        .getContentAsString();

    Long categoryId = objectMapper.readTree(categoryJson).get("id").asLong();

    mockMvc.perform(delete("/categories/{id}/nameTranslation", categoryId)).andExpect(status().isOk());

    mockMvc
        .perform(get("/categories/{id}/nameTranslation", categoryId))
        .andExpect(status().isOk())
        .andExpect(content().string(""));
  }
}
