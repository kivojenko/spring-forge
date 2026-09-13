package com.kivojenko.spring.forge.example.controller;

import com.kivojenko.spring.forge.example.WithPostgres;
import com.kivojenko.spring.forge.example.model.general.Category;
import com.kivojenko.spring.forge.example.model.general.Translation;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
            .content(objectMapper.writeValueAsString(Category.builder().name("ItemCategory 1").build())))
        .andExpect(status().isCreated());

    mockMvc.perform(get("/categories/count")).andExpect(status().isOk()).andExpect(jsonPath("$", is(1)));
  }

  @Test
  void testBaseCreate() throws Exception {
    mockMvc
        .perform(post("/categories")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Category.builder().name("New ItemCategory").build())))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.name", is("New ItemCategory")))
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
        .perform(post("/categories/get-or-create").param("name", "Unique ItemCategory"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name", is("Unique ItemCategory")))
        .andExpect(jsonPath("$.id").exists());

    mockMvc.perform(get("/categories/count")).andExpect(status().isOk()).andExpect(jsonPath("$", is(1)));

    // The second call returns the same category
    mockMvc
        .perform(post("/categories/get-or-create").param("name", "Unique ItemCategory"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name", is("Unique ItemCategory")))
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
                .name("Translated ItemCategory")
                .nameTranslation(Translation.builder().enUS("ItemCategory Name").build())
                .build())))
        .andExpect(status().isCreated())
        .andReturn()
        .getResponse()
        .getContentAsString();

    Long categoryId = objectMapper.readTree(categoryJson).get("id").asLong();

    mockMvc
        .perform(get("/categories/{id}/nameTranslation", categoryId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.enUS", is("ItemCategory Name")));
  }

  @Test
  void testRemoveNameTranslation() throws Exception {
    String categoryJson = mockMvc
        .perform(post("/categories")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Category
                .builder()
                .name("ItemCategory with Translation")
                .nameTranslation(Translation.builder().enUS("ItemCategory Name").build())
                .build())))
        .andExpect(status().isCreated())
        .andReturn()
        .getResponse()
        .getContentAsString();

    Long categoryId = objectMapper.readTree(categoryJson).get("id").asLong();

    mockMvc.perform(delete("/categories/{id}/nameTranslation", categoryId)).andExpect(status().isNoContent());

    mockMvc
        .perform(get("/categories/{id}/nameTranslation", categoryId))
        .andExpect(status().isOk())
        .andExpect(content().string(""));
  }

  @Test
  void testAddExistingParentAndGetParents() throws Exception {
    String parentJson = mockMvc
        .perform(post("/categories")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Category.builder().name("Parent Cat").build())))
        .andExpect(status().isCreated())
        .andReturn()
        .getResponse()
        .getContentAsString();
    Long parentId = objectMapper.readTree(parentJson).get("id").asLong();

    String childJson = mockMvc
        .perform(post("/categories")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Category.builder().name("Child Cat").build())))
        .andExpect(status().isCreated())
        .andReturn()
        .getResponse()
        .getContentAsString();
    Long childId = objectMapper.readTree(childJson).get("id").asLong();

    // Link parent to child via PUT /categories/{childId}/parents/{parentId}
    mockMvc.perform(put("/categories/{id}/parents/{categoryId}", childId, parentId))
        .andExpect(status().isNoContent());

    // Verify GET /categories/{childId}/parents
    mockMvc.perform(get("/categories/{id}/parents", childId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(1)))
        .andExpect(jsonPath("$[0].id", is(parentId.intValue())))
        .andExpect(jsonPath("$[0].name", is("Parent Cat")));
  }

  @Test
  void testAddNewParent() throws Exception {
    String childJson = mockMvc
        .perform(post("/categories")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Category.builder().name("Child for New Parent").build())))
        .andExpect(status().isCreated())
        .andReturn()
        .getResponse()
        .getContentAsString();
    Long childId = objectMapper.readTree(childJson).get("id").asLong();

    // Add new parent via POST /categories/{childId}/parents
    String newParentJson = mockMvc
        .perform(post("/categories/{id}/parents", childId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Category.builder().name("Created Parent").build())))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.name", is("Created Parent")))
        .andExpect(jsonPath("$.id").exists())
        .andReturn()
        .getResponse()
        .getContentAsString();
    Long newParentId = objectMapper.readTree(newParentJson).get("id").asLong();

    // Verify GET /categories/{childId}/parents
    mockMvc.perform(get("/categories/{id}/parents", childId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(1)))
        .andExpect(jsonPath("$[0].id", is(newParentId.intValue())))
        .andExpect(jsonPath("$[0].name", is("Created Parent")));
  }

  @Test
  void testRemoveRelationWithParent() throws Exception {
    String parentJson = mockMvc
        .perform(post("/categories")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Category.builder().name("Parent to Remove").build())))
        .andExpect(status().isCreated())
        .andReturn()
        .getResponse()
        .getContentAsString();
    Long parentId = objectMapper.readTree(parentJson).get("id").asLong();

    String childJson = mockMvc
        .perform(post("/categories")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Category.builder().name("Child to Unlink Parent").build())))
        .andExpect(status().isCreated())
        .andReturn()
        .getResponse()
        .getContentAsString();
    Long childId = objectMapper.readTree(childJson).get("id").asLong();

    mockMvc.perform(put("/categories/{id}/parents/{categoryId}", childId, parentId))
        .andExpect(status().isNoContent());

    // Unlink parent via DELETE /categories/{childId}/parents/{parentId}
    mockMvc.perform(delete("/categories/{id}/parents/{categoryId}", childId, parentId))
        .andExpect(status().isNoContent());

    // Verify GET /categories/{childId}/parents is empty
    mockMvc.perform(get("/categories/{id}/parents", childId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(0)));
  }

  @Test
  void testAddExistingChildAndGetChildren() throws Exception {
    String parentJson = mockMvc
        .perform(post("/categories")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Category.builder().name("Parent Cat 2").build())))
        .andExpect(status().isCreated())
        .andReturn()
        .getResponse()
        .getContentAsString();
    Long parentId = objectMapper.readTree(parentJson).get("id").asLong();

    String childJson = mockMvc
        .perform(post("/categories")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Category.builder().name("Child Cat 2").build())))
        .andExpect(status().isCreated())
        .andReturn()
        .getResponse()
        .getContentAsString();
    Long childId = objectMapper.readTree(childJson).get("id").asLong();

    // Link child to parent via PUT /categories/{parentId}/children/{childId}
    mockMvc.perform(put("/categories/{id}/children/{categoryId}", parentId, childId))
        .andExpect(status().isNoContent());

    // Verify GET /categories/{parentId}/children
    mockMvc.perform(get("/categories/{id}/children", parentId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(1)))
        .andExpect(jsonPath("$[0].id", is(childId.intValue())))
        .andExpect(jsonPath("$[0].name", is("Child Cat 2")));
  }

  @Test
  void testAddNewChild() throws Exception {
    String parentJson = mockMvc
        .perform(post("/categories")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Category.builder().name("Parent for New Child").build())))
        .andExpect(status().isCreated())
        .andReturn()
        .getResponse()
        .getContentAsString();
    Long parentId = objectMapper.readTree(parentJson).get("id").asLong();

    // Add new child via POST /categories/{parentId}/children
    String newChildJson = mockMvc
        .perform(post("/categories/{id}/children", parentId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Category.builder().name("Created Child").build())))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.name", is("Created Child")))
        .andExpect(jsonPath("$.id").exists())
        .andReturn()
        .getResponse()
        .getContentAsString();
    Long newChildId = objectMapper.readTree(newChildJson).get("id").asLong();

    // Verify GET /categories/{parentId}/children
    mockMvc.perform(get("/categories/{id}/children", parentId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(1)))
        .andExpect(jsonPath("$[0].id", is(newChildId.intValue())))
        .andExpect(jsonPath("$[0].name", is("Created Child")));
  }

  @Test
  void testRemoveRelationWithChild() throws Exception {
    String parentJson = mockMvc
        .perform(post("/categories")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Category.builder().name("Parent to Remove Child").build())))
        .andExpect(status().isCreated())
        .andReturn()
        .getResponse()
        .getContentAsString();
    Long parentId = objectMapper.readTree(parentJson).get("id").asLong();

    String childJson = mockMvc
        .perform(post("/categories")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Category.builder().name("Child to Unlink").build())))
        .andExpect(status().isCreated())
        .andReturn()
        .getResponse()
        .getContentAsString();
    Long childId = objectMapper.readTree(childJson).get("id").asLong();

    mockMvc.perform(put("/categories/{id}/children/{categoryId}", parentId, childId))
        .andExpect(status().isNoContent());

    // Unlink child via DELETE /categories/{parentId}/children/{childId}
    mockMvc.perform(delete("/categories/{id}/children/{categoryId}", parentId, childId))
        .andExpect(status().isNoContent());

    // Verify GET /categories/{parentId}/children is empty
    mockMvc.perform(get("/categories/{id}/children", parentId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(0)));
  }
}
