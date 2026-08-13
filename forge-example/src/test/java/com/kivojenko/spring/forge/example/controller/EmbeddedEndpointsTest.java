package com.kivojenko.spring.forge.example.controller;

import com.kivojenko.spring.forge.example.WithPostgres;
import com.kivojenko.spring.forge.example.model.embedded.Address;
import com.kivojenko.spring.forge.example.model.embedded.Customer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
public class EmbeddedEndpointsTest extends WithPostgres {

  @Test
  void shouldAddEmbeddedViaPost() throws Exception {
    // Create base entity
    var createResp = mockMvc.perform(post("/customers")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Customer.builder()
                .name("Alice")
                .build())))
        .andExpect(status().isCreated())
        .andReturn();

    var created = objectMapper.readTree(createResp.getResponse().getContentAsString());
    long id = created.get("id").asLong();

    // POST embedded value
    mockMvc.perform(post("/customers/{id}/address", id)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Address.builder()
                .street("Main St")
                .city("Tallinn")
                .build())))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.city", is("Tallinn")));

    // Verify via GET
    mockMvc.perform(get("/customers/{id}/address", id))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.street", is("Main St")))
        .andExpect(jsonPath("$.city", is("Tallinn")));
  }
}
