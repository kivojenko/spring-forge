package com.kivojenko.spring.forge.example.controller;

import com.kivojenko.spring.forge.example.WithPostgres;
import com.kivojenko.spring.forge.example.model.general.Category;
import com.kivojenko.spring.forge.example.model.general.Translation;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
public class OneToOneEndpointsTest extends WithPostgres {

  @Test
  void shouldCreateAndLinkOneToOneViaPost() throws Exception {
    // Create base entity
    var createResp = mockMvc.perform(post("/categories")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Category.builder()
                .name("Sci-Fi")
                .build())))
        .andExpect(status().isCreated())
        .andReturn();

    var created = objectMapper.readTree(createResp.getResponse().getContentAsString());
    long id = created.get("id").asLong();

    // POST one-to-one value (entity)
    mockMvc.perform(post("/categories/{id}/nameTranslation", id)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Translation.builder()
                .enUS("Science Fiction")
                .ruRU("Научная фантастика")
                .build())))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.enUS", is("Science Fiction")));

    // Verify via GET
    mockMvc.perform(get("/categories/{id}/nameTranslation", id))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.enUS", is("Science Fiction")))
        .andExpect(jsonPath("$.ruRU", is("Научная фантастика")));
  }
}
