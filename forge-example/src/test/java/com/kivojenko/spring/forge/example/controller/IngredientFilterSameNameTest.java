package com.kivojenko.spring.forge.example.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.kivojenko.spring.forge.example.WithPostgres;
import java.lang.reflect.Field;
import java.util.Arrays;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;

@SpringBootTest
@AutoConfigureMockMvc
public class IngredientFilterSameNameTest extends WithPostgres {

  private long createIngredient(String name) throws Exception {
    var body = "{" +
        "\"name\": \"" + name + "\"" +
        "}";
    var resp = mockMvc.perform(post("/ingredients")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isCreated())
        .andReturn();
    JsonNode json = objectMapper.readTree(resp.getResponse().getContentAsString());
    return json.get("id").asLong();
  }

  private void addAltName(long ingredientId, String name) throws Exception {
    var body = "{" +
        "\"name\": \"" + name + "\"" +
        "}";
    mockMvc.perform(post("/ingredients/" + ingredientId + "/alternativeNames")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isCreated());
  }

  @BeforeEach
  void setUp() throws Exception {
    long i1 = createIngredient("Cumin");
    addAltName(i1, "Jeera");

    long i2 = createIngredient("Caraway");
    addAltName(i2, "Cumin Seeds");
  }

  @AfterEach
  void cleanUpIngredients() throws Exception {
    // Delete via direct ID query to avoid brittle JSON parsing on pageable response
    var ids = jdbcTemplate.queryForList("SELECT id FROM ingredients", Long.class);
    for (Long id : ids) {
      mockMvc.perform(delete("/ingredients/{id}", id)).andExpect(status().isNoContent());
    }
  }

  @Test
  void reflection_shows_only_single_name_field_in_generated_filter() throws Exception {
    Class<?> filterClass = Class.forName("com.kivojenko.spring.forge.example.model.filter.IngredientForgeFilter");
    Field[] fields = filterClass.getDeclaredFields();
    long nameStringFields = Arrays.stream(fields)
        .filter(f -> f.getName().equals("name") && f.getType().equals(String.class))
        .count();
    // Current behavior: duplicate filter names are deduplicated -> only one remains
    org.junit.jupiter.api.Assertions.assertEquals(1, nameStringFields,
        "Expected only one 'name' field in the generated filter DTO");
  }

  @Test
  void name_filter_combines_primary_and_alternative_with_or() throws Exception {
    // Searching by "cumin" should match by primary name and by alternative name
    mockMvc.perform(get("/ingredients").param("name", "cumin"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(2)))
        .andExpect(jsonPath("$.content[*].name", org.hamcrest.Matchers.hasItems("Cumin", "Caraway")));

    // Searching by an alternative name should still match the owning entity
    mockMvc.perform(get("/ingredients").param("name", "jeer"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(1)))
        .andExpect(jsonPath("$.content[0].name", is("Cumin")));
  }
}
