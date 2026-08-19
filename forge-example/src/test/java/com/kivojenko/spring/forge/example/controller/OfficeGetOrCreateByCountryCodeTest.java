package com.kivojenko.spring.forge.example.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.kivojenko.spring.forge.example.WithPostgres;
import com.kivojenko.spring.forge.example.model.filter.Country;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
public class OfficeGetOrCreateByCountryCodeTest extends WithPostgres {

  @BeforeEach
  void cleanLocalData() throws Exception {
    // Delete existing offices
    String officesJson = mockMvc.perform(get("/offices")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    JsonNode officesRoot = objectMapper.readTree(officesJson);
    for (JsonNode office : officesRoot.get("content")) {
      Long officeId = office.get("id").asLong();
      mockMvc.perform(delete("/offices/{id}", officeId)).andExpect(status().isNoContent());
    }

    // Delete existing countries
    String countriesJson = mockMvc.perform(get("/countries")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    JsonNode countriesRoot = objectMapper.readTree(countriesJson);
    for (JsonNode c : countriesRoot.get("content")) {
      String code = c.get("code").asText();
      mockMvc.perform(delete("/countries/{id}", code)).andExpect(status().isNoContent());
    }

    // Seed a couple of countries
    createCountry("EE", "Estonia");
    createCountry("DE", "Germany");
  }

  private void createCountry(String code, String name) throws Exception {
    mockMvc.perform(post("/countries")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Country.builder().code(code).name(name).build())))
        .andExpect(status().isCreated());
  }

  @Test
  void testGetOrCreateByCountryCode() throws Exception {
    // First call should create an office linked to EE
    mockMvc.perform(post("/offices/get-or-create").param("country.code", "EE"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.country.code", is("EE")))
        .andExpect(jsonPath("$.id").exists());

    mockMvc.perform(get("/offices/count")).andExpect(status().isOk()).andExpect(jsonPath("$", is(1)));

    // Second call must return the same (no new office)
    mockMvc.perform(post("/offices/get-or-create").param("country.code", "EE"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.country.code", is("EE")))
        .andExpect(jsonPath("$.id").exists());

    mockMvc.perform(get("/offices/count")).andExpect(status().isOk()).andExpect(jsonPath("$", is(1)));
  }
}
