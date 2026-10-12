package com.kivojenko.spring.forge.example.controller;

import com.kivojenko.spring.forge.example.WithPostgres;
import com.kivojenko.spring.forge.example.model.filter.ProductForgeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
public class ProductMultipleGetOrCreateTest extends WithPostgres {

  @Autowired
  private ProductForgeService productService;

  @Test
  void testGetOrCreateByNameSharesPathWithSku() throws Exception {
    mockMvc
        .perform(post("/products/get-or-create").param("name", "Widget"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name", is("Widget")))
        .andExpect(jsonPath("$.id").exists());

    mockMvc
        .perform(post("/products/get-or-create").param("name", "Widget"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name", is("Widget")));

    mockMvc.perform(get("/products/count")).andExpect(status().isOk()).andExpect(jsonPath("$", is(1)));

    // ignoreCase = false on the name lookup: a different case is a different product
    mockMvc
        .perform(post("/products/get-or-create").param("name", "widget"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name", is("widget")));

    mockMvc.perform(get("/products/count")).andExpect(status().isOk()).andExpect(jsonPath("$", is(2)));

    mockMvc
        .perform(post("/products/get-or-create").param("sku", "SKU-1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.sku", is("SKU-1")));

    mockMvc.perform(get("/products/count")).andExpect(status().isOk()).andExpect(jsonPath("$", is(3)));
  }

  @Test
  void testServiceMethodsAreNamedPerField() {
    var bySku = productService.getOrCreateBySku("SKU-42");
    var byName = productService.getOrCreateByName("Gadget");

    assertEquals(bySku.getId(), productService.getOrCreateBySku("sku-42").getId());
    assertEquals(byName.getId(), productService.getOrCreateByName("Gadget").getId());
  }
}
