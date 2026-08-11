package com.kivojenko.spring.forge.example.controller;

import com.kivojenko.spring.forge.example.WithPostgres;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
public class ProductGetOrCreateBySkuTest extends WithPostgres {

  @Test
  void testGetOrCreateBySku() throws Exception {
    mockMvc
        .perform(post("/products/get-or-create").param("sku", "SKU-123"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.sku", is("SKU-123")))
        .andExpect(jsonPath("$.id").exists());

    mockMvc.perform(get("/products/count")).andExpect(status().isOk()).andExpect(jsonPath("$", is(1)));

    // Second call with the same SKU should return the existing product
    mockMvc
        .perform(post("/products/get-or-create").param("sku", "SKU-123"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.sku", is("SKU-123")))
        .andExpect(jsonPath("$.id").exists());

    mockMvc.perform(get("/products/count")).andExpect(status().isOk()).andExpect(jsonPath("$", is(1)));
  }

  @Test
  void testGetOrCreateBySkuIgnoreCase() throws Exception {
    // Create using mixed case
    mockMvc
        .perform(post("/products/get-or-create").param("sku", "AbC-999"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.sku", is("AbC-999")))
        .andExpect(jsonPath("$.id").exists());

    // Call again with different case, should find existing due to IgnoreCase
    mockMvc
        .perform(post("/products/get-or-create").param("sku", "abc-999"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.sku", is("AbC-999")))
        .andExpect(jsonPath("$.id").exists());

    mockMvc.perform(get("/products/count")).andExpect(status().isOk()).andExpect(jsonPath("$", is(1)));
  }
}
