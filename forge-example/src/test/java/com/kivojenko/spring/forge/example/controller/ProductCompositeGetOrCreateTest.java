package com.kivojenko.spring.forge.example.controller;

import com.kivojenko.spring.forge.example.WithPostgres;
import com.kivojenko.spring.forge.example.model.filter.ProductForgeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
public class ProductCompositeGetOrCreateTest extends WithPostgres {

  @Autowired
  private ProductForgeService productService;

  @Test
  void testGetOrCreateByBrandAndNameEndpoint() throws Exception {
    mockMvc
        .perform(post("/products/get-or-create").param("brand", "Acme").param("name", "Anvil"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.brand", is("Acme")))
        .andExpect(jsonPath("$.name", is("Anvil")))
        .andExpect(jsonPath("$.id").exists());

    // Same key, different case: found, not created
    mockMvc
        .perform(post("/products/get-or-create").param("brand", "ACME").param("name", "anvil"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.brand", is("Acme")));

    mockMvc.perform(get("/products/count")).andExpect(status().isOk()).andExpect(jsonPath("$", is(1)));

    // Only one part of the key matches: a new product
    mockMvc
        .perform(post("/products/get-or-create").param("brand", "Globex").param("name", "Anvil"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.brand", is("Globex")));

    mockMvc.perform(get("/products/count")).andExpect(status().isOk()).andExpect(jsonPath("$", is(2)));
  }

  @Test
  void testServiceMethodIsNamedAfterAllFields() {
    var first = productService.getOrCreateByBrandAndName("Acme", "Rocket");
    var same = productService.getOrCreateByBrandAndName("acme", "ROCKET");
    var other = productService.getOrCreateByBrandAndName("Acme", "Magnet");

    assertEquals(first.getId(), same.getId());
    assertNotEquals(first.getId(), other.getId());
  }
}
