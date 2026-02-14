package com.kivojenko.spring.forge.example.controller;

import com.kivojenko.spring.forge.example.WithPostgres;
import com.kivojenko.spring.forge.example.model.Author;
import com.kivojenko.spring.forge.example.model.Book;
import com.kivojenko.spring.forge.example.model.Category;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class AuthorControllerTest extends WithPostgres {

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
  void testGetOrCreate() throws Exception {
    mockMvc
        .perform(post("/authors/get-or-create").param("name", "John Doe"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name", is("John Doe")));

    // Call again, should return the same author
    mockMvc
        .perform(post("/authors/get-or-create").param("name", "John Doe"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name", is("John Doe")));

    mockMvc.perform(get("/authors/count")).andExpect(status().isOk()).andExpect(jsonPath("$", is(1)));
  }

  @Test
  void testFindAll() throws Exception {
    mockMvc
        .perform(post("/authors")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Author.builder().name("Author 1").build())))
        .andExpect(status().isCreated());

    mockMvc
        .perform(post("/authors")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Author.builder().name("Author 2").build())))
        .andExpect(status().isCreated());

    mockMvc.perform(get("/authors")).andExpect(status().isOk()).andExpect(jsonPath("$.content", hasSize(2)));
  }

  @Test
  void testGetBooks() throws Exception {
    Long categoryId = getOrCreateCategory("Cat");
    String authorJson = mockMvc
        .perform(post("/authors")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Author.builder().name("Author with books").build())))
        .andExpect(status().isCreated())
        .andReturn()
        .getResponse()
        .getContentAsString();

    Long authorId = objectMapper.readTree(authorJson).get("id").asLong();

    mockMvc
        .perform(post("/authors/{id}/books", authorId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Book
                .builder()
                .title("Book 1")
                .categories(List.of(Category.builder().id(categoryId).build()))
                .build())))
        .andExpect(status().isOk());

    mockMvc
        .perform(get("/authors/{id}/books", authorId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].title", is("Book 1")));
  }

  @Test
  void testGetBooksTitles() throws Exception {
    Long categoryId = getOrCreateCategory("Cat");
    String authorJson = mockMvc
        .perform(post("/authors")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Author.builder().name("Author with book titles").build())))
        .andExpect(status().isCreated())
        .andReturn()
        .getResponse()
        .getContentAsString();

    Long authorId = objectMapper.readTree(authorJson).get("id").asLong();

    mockMvc
        .perform(post("/authors/{id}/books", authorId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Book
                .builder()
                .title("Book Title 1")
                .categories(List.of(Category.builder().id(categoryId).build()))
                .build())))
        .andExpect(status().isOk());

    mockMvc
        .perform(post("/authors/{id}/books", authorId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Book
                .builder()
                .title("Book Title 2")
                .categories(List.of(Category.builder().id(categoryId).build()))
                .build())))
        .andExpect(status().isOk());

    mockMvc
        .perform(get("/authors/{id}/booksTitles", authorId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(2)))
        .andExpect(jsonPath("$[0]", is("Book Title 1")))
        .andExpect(jsonPath("$[1]", is("Book Title 2")));
  }

  @Test
  void testAddNewBook() throws Exception {
    Long categoryId = getOrCreateCategory("New Cat");
    String authorJson = mockMvc
        .perform(post("/authors")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Author.builder().name("Author for new book").build())))
        .andExpect(status().isCreated())
        .andReturn()
        .getResponse()
        .getContentAsString();

    Long authorId = objectMapper.readTree(authorJson).get("id").asLong();

    mockMvc
        .perform(post("/authors/{id}/books", authorId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Book
                .builder()
                .title("New Book")
                .categories(List.of(Category.builder().id(categoryId).build()))
                .build())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.title", is("New Book")));

    mockMvc.perform(get("/books/count")).andExpect(status().isOk()).andExpect(jsonPath("$", is(1)));
  }

  @Test
  void testRemoveBooks() throws Exception {
    Long categoryId = getOrCreateCategory("Cat");
    String authorJson = mockMvc
        .perform(post("/authors")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Author.builder().name("Author for remove").build())))
        .andExpect(status().isCreated())
        .andReturn()
        .getResponse()
        .getContentAsString();

    Long authorId = objectMapper.readTree(authorJson).get("id").asLong();

    String bookJson = mockMvc
        .perform(post("/authors/{id}/books", authorId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Book
                .builder()
                .title("Book to remove")
                .categories(List.of(Category.builder().id(categoryId).build()))
                .build())))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();

    Long bookId = objectMapper.readTree(bookJson).get("id").asLong();

    mockMvc.perform(delete("/authors/{id}/books/{subId}", authorId, bookId)).andExpect(status().isOk());

    mockMvc
        .perform(get("/authors/{id}/books", authorId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(0)));
  }

  @Test
  void testBaseCount() throws Exception {
    mockMvc
        .perform(post("/authors")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Author.builder().name("Author 1").build())))
        .andExpect(status().isCreated());

    mockMvc.perform(get("/authors/count")).andExpect(status().isOk()).andExpect(jsonPath("$", is(1)));
  }

  @Test
  void testBaseCreate() throws Exception {
    mockMvc
        .perform(post("/authors")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Author.builder().name("New Author").build())))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.name", is("New Author")))
        .andExpect(jsonPath("$.id").exists());
  }

  @Test
  void testBaseGetById() throws Exception {
    String json = mockMvc
        .perform(post("/authors")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Author.builder().name("Get Me").build())))
        .andExpect(status().isCreated())
        .andReturn()
        .getResponse()
        .getContentAsString();

    Long id = objectMapper.readTree(json).get("id").asLong();

    mockMvc.perform(get("/authors/{id}", id)).andExpect(status().isOk()).andExpect(jsonPath("$.name", is("Get Me")));
  }

  @Test
  void testBaseUpdate() throws Exception {
    String json = mockMvc
        .perform(post("/authors")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Author.builder().name("Old Name").build())))
        .andExpect(status().isCreated())
        .andReturn()
        .getResponse()
        .getContentAsString();

    Long id = objectMapper.readTree(json).get("id").asLong();

    mockMvc
        .perform(put("/authors/{id}", id)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Author.builder().name("New Name").build())))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.name", is("New Name")));
  }

  @Test
  void testBaseDelete() throws Exception {
    String json = mockMvc
        .perform(post("/authors")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Author.builder().name("To Delete").build())))
        .andExpect(status().isCreated())
        .andReturn()
        .getResponse()
        .getContentAsString();

    Long id = objectMapper.readTree(json).get("id").asLong();

    mockMvc.perform(delete("/authors/{id}", id)).andExpect(status().isNoContent());

    assertThrows(ServletException.class, () -> mockMvc.perform(get("/authors/{id}", id)));
  }
}