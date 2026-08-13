package com.kivojenko.spring.forge.example.controller;

import com.kivojenko.spring.forge.example.WithPostgres;
import com.kivojenko.spring.forge.example.model.general.Author;
import com.kivojenko.spring.forge.example.model.general.Book;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
public class ManyToOneAddNewEndpointsTest extends WithPostgres {

  @Test
  void shouldCreateAndLinkManyToOneViaPost() throws Exception {
    // Create base entity (Book)
    var createResp = mockMvc.perform(post("/books")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Book.builder()
                .title("Dune")
                .build())))
        .andExpect(status().isCreated())
        .andReturn();

    var created = objectMapper.readTree(createResp.getResponse().getContentAsString());
    long id = created.get("id").asLong();

    // POST many-to-one value (Author)
    mockMvc.perform(post("/books/{id}/author", id)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Author.builder()
                .name("Frank Herbert")
                .build())))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.name", is("Frank Herbert")));

    // Verify via GET
    mockMvc.perform(get("/books/{id}/author", id))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name", is("Frank Herbert")));
  }
}
