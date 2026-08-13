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
public class CarGetOrCreateInheritedNameTest extends WithPostgres {

  @Test
  void testGetOrCreateByInheritedName() throws Exception {
    mockMvc
        .perform(post("/cars/get-or-create").param("name", "Focus"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name", is("Focus")))
        .andExpect(jsonPath("$.id").exists());

    mockMvc.perform(get("/cars/count")).andExpect(status().isOk()).andExpect(jsonPath("$", is(1)));

    // Second call with the same name should return the existing car
    mockMvc
        .perform(post("/cars/get-or-create").param("name", "Focus"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name", is("Focus")))
        .andExpect(jsonPath("$.id").exists());

    mockMvc.perform(get("/cars/count")).andExpect(status().isOk()).andExpect(jsonPath("$", is(1)));
  }
}
