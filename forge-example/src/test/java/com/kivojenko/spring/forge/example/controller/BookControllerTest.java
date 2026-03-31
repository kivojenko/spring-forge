package com.kivojenko.spring.forge.example.controller;

import com.kivojenko.spring.forge.example.WithPostgres;
import com.kivojenko.spring.forge.example.model.general.Author;
import com.kivojenko.spring.forge.example.model.general.Book;
import com.kivojenko.spring.forge.example.model.general.Category;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class BookControllerTest extends WithPostgres {

  private Long getOrCreateCategory(String name) throws Exception {
    String categoryJson = mockMvc
        .perform(post("/categories/get-or-create").param("name", name))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();
    return objectMapper.readTree(categoryJson).get("id").asLong();
  }

  @Test
  void testFindAll() throws Exception {
    Long categoryId1 = getOrCreateCategory("Cat 1");
    Long categoryId2 = getOrCreateCategory("Cat 2");

    mockMvc
        .perform(post("/books")
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Book
                .builder()
                .title("Book 1")
                .categories(List.of(Category.builder().id(categoryId1).build()))
                .build())))
        .andExpect(status().isCreated());

    mockMvc
        .perform(post("/books")
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Book
                .builder()
                .title("Book 2")
                .categories(List.of(Category.builder().id(categoryId2).build()))
                .build())))
        .andExpect(status().isCreated());

    mockMvc.perform(get("/books")).andExpect(status().isOk()).andExpect(jsonPath("$.content", hasSize(2)));
  }

  @Test
  void testAddExistingCategory() throws Exception {
    Long categoryId = getOrCreateCategory("Cat");
    String authorJson = mockMvc
        .perform(post("/authors")
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Author.builder().name("Existing Author").build())))
        .andExpect(status().isCreated())
        .andReturn()
        .getResponse()
        .getContentAsString();

    Long authorId = objectMapper.readTree(authorJson).get("id").asLong();

    String bookJson = mockMvc
        .perform(post("/books")
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Book.builder().title("Book without category").build())))
        .andExpect(status().isCreated())
        .andReturn()
        .getResponse()
        .getContentAsString();

    Long bookId = objectMapper.readTree(bookJson).get("id").asLong();

    mockMvc.perform(put("/books/{id}/categories/{subId}", bookId, categoryId)).andExpect(status().isNoContent());

    mockMvc
        .perform(get("/books/{id}/categories", bookId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].id", is(categoryId.intValue())));
  }

  @Test
  void testRemoveCategory() throws Exception {
    Long categoryId = getOrCreateCategory("Cat");
    String bookJson = mockMvc
        .perform(post("/books")
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Book
                .builder()
                .title("Book with category")
                .categories(List.of(Category.builder().id(categoryId).build()))
                .build())))
        .andExpect(status().isCreated())
        .andReturn()
        .getResponse()
        .getContentAsString();

    Long bookId = objectMapper.readTree(bookJson).get("id").asLong();

    mockMvc.perform(delete("/books/{id}/categories/{subId}", bookId, categoryId)).andExpect(status().isNoContent());

    mockMvc
        .perform(get("/books/{id}/categories", bookId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(0)));
  }

  @Test
  void testGetAuthor() throws Exception {
    Long categoryId = getOrCreateCategory("Cat 1");
    String authorJson = mockMvc
        .perform(post("/authors")
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Author.builder().name("Author 1").build())))
        .andExpect(status().isCreated())
        .andReturn()
        .getResponse()
        .getContentAsString();

    Long authorId = objectMapper.readTree(authorJson).get("id").asLong();

    String bookJson = mockMvc
        .perform(post("/authors/{id}/books", authorId)
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Book
                .builder()
                .title("Book 1")
                .categories(List.of(Category.builder().id(categoryId).build()))
                .build())))
        .andExpect(status().isCreated())
        .andReturn()
        .getResponse()
        .getContentAsString();

    Long bookId = objectMapper.readTree(bookJson).get("id").asLong();

    mockMvc
        .perform(get("/books/{id}/author", bookId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name", is("Author 1")));
  }

  @Test
  void testAddExistingAuthor() throws Exception {
    Long categoryId = getOrCreateCategory("Cat");
    String authorJson = mockMvc
        .perform(post("/authors")
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Author.builder().name("Existing Author").build())))
        .andExpect(status().isCreated())
        .andReturn()
        .getResponse()
        .getContentAsString();

    Long authorId = objectMapper.readTree(authorJson).get("id").asLong();

    String bookJson = mockMvc
        .perform(post("/books")
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Book
                .builder()
                .title("Book without author")
                .categories(List.of(Category.builder().id(categoryId).build()))
                .build())))
        .andExpect(status().isCreated())
        .andReturn()
        .getResponse()
        .getContentAsString();

    Long bookId = objectMapper.readTree(bookJson).get("id").asLong();

    mockMvc.perform(put("/books/{id}/author/{subId}", bookId, authorId)).andExpect(status().isOk());

    mockMvc
        .perform(get("/books/{id}/author", bookId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id", is(authorId.intValue())));
  }

  @Test
  void testRemoveAuthor() throws Exception {
    Long categoryId = getOrCreateCategory("Cat");
    String authorJson = mockMvc
        .perform(post("/authors")
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Author.builder().name("Author to remove").build())))
        .andExpect(status().isCreated())
        .andReturn()
        .getResponse()
        .getContentAsString();

    Long authorId = objectMapper.readTree(authorJson).get("id").asLong();

    String bookJson = mockMvc
        .perform(post("/authors/{id}/books", authorId)
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Book
                .builder()
                .title("Book with author")
                .categories(List.of(Category.builder().id(categoryId).build()))
                .build())))
        .andExpect(status().isCreated())
        .andReturn()
        .getResponse()
        .getContentAsString();

    Long bookId = objectMapper.readTree(bookJson).get("id").asLong();

    mockMvc.perform(delete("/books/{id}/author/{subId}", bookId, authorId)).andExpect(status().isNoContent());

    mockMvc.perform(get("/books/{id}", bookId)).andExpect(status().isOk()).andExpect(jsonPath("$.author").isEmpty());
  }

  @Test
  void testBaseCount() throws Exception {
    Long categoryId = getOrCreateCategory("Cat 1");
    mockMvc
        .perform(post("/books")
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Book
                .builder()
                .title("Book 1")
                .categories(List.of(Category.builder().id(categoryId).build()))
                .build())))
        .andExpect(status().isCreated());

    mockMvc.perform(get("/books/count")).andExpect(status().isOk()).andExpect(jsonPath("$", is(1)));
  }

  @Test
  void testBaseCreate() throws Exception {
    Long categoryId = getOrCreateCategory("New Cat");
    mockMvc
        .perform(post("/books")
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Book
                .builder()
                .title("New Book")
                .categories(List.of(Category.builder().id(categoryId).build()))
                .build())))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.title", is("New Book")))
        .andExpect(jsonPath("$.id").exists());
  }

  @Test
  void testBaseGetById() throws Exception {
    Long categoryId = getOrCreateCategory("Cat");
    String json = mockMvc
        .perform(post("/books")
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Book
                .builder()
                .title("Get Me")
                .categories(List.of(Category.builder().id(categoryId).build()))
                .build())))
        .andExpect(status().isCreated())
        .andReturn()
        .getResponse()
        .getContentAsString();

    Long id = objectMapper.readTree(json).get("id").asLong();

    mockMvc.perform(get("/books/{id}", id)).andExpect(status().isOk()).andExpect(jsonPath("$.title", is("Get Me")));
  }

  @Test
  void testBaseUpdate() throws Exception {
    Long categoryId = getOrCreateCategory("Cat");
    String json = mockMvc
        .perform(post("/books")
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Book
                .builder()
                .title("Old Title")
                .categories(List.of(Category.builder().id(categoryId).build()))
                .build())))
        .andExpect(status().isCreated())
        .andReturn()
        .getResponse()
        .getContentAsString();

    Long id = objectMapper.readTree(json).get("id").asLong();

    mockMvc
        .perform(put("/books/{id}", id)
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Book
                .builder()
                .title("New Title")
                .categories(List.of(Category.builder().id(categoryId).build()))
                .build())))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.title", is("New Title")));
  }

  @Test
  void testBaseDelete() throws Exception {
    Long categoryId = getOrCreateCategory("Cat");
    String json = mockMvc
        .perform(post("/books")
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Book
                .builder()
                .title("To Delete")
                .categories(List.of(Category.builder().id(categoryId).build()))
                .build())))
        .andExpect(status().isCreated())
        .andReturn()
        .getResponse()
        .getContentAsString();

    Long id = objectMapper.readTree(json).get("id").asLong();

    mockMvc.perform(delete("/books/{id}", id)).andExpect(status().isNoContent());

    assertThrows(ServletException.class, () -> mockMvc.perform(get("/books/{id}", id)));
  }
}
