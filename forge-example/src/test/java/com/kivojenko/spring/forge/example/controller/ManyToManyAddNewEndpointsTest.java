package com.kivojenko.spring.forge.example.controller;

import com.kivojenko.spring.forge.example.WithPostgres;
import com.kivojenko.spring.forge.example.model.general.Book;
import com.kivojenko.spring.forge.example.model.general.Category;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
public class ManyToManyAddNewEndpointsTest extends WithPostgres {

  @Test
  void shouldCreateAndLinkManyToManyViaPost() throws Exception {
    // Create base entity (Book)
    var createResp = mockMvc.perform(post("/books")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Book.builder()
                .title("The Hobbit")
                .build())))
        .andExpect(status().isCreated())
        .andReturn();

    var created = objectMapper.readTree(createResp.getResponse().getContentAsString());
    long id = created.get("id").asLong();

    // POST many-to-many value (Category)
    mockMvc.perform(post("/books/{id}/categories", id)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Category.builder()
                .name("Adventure")
                .build())))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.name", is("Adventure")));

    // Verify via GET
    mockMvc.perform(get("/books/{id}/categories", id))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].name", is("Adventure")));
  }
}
