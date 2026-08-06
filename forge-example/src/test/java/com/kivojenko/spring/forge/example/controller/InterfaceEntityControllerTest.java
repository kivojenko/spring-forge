package com.kivojenko.spring.forge.example.controller;

import com.kivojenko.spring.forge.example.WithPostgres;
import com.kivojenko.spring.forge.example.model.InterfaceEntity;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class InterfaceEntityControllerTest extends WithPostgres {

    @Test
    void testCreateAndGet() throws Exception {
        InterfaceEntity entity = new InterfaceEntity();
        entity.setName("Test Entity");

        String json = mockMvc.perform(post("/interfaceEntities")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(entity)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is("Test Entity")))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long id = objectMapper.readTree(json).get("id").asLong();

        mockMvc.perform(get("/interfaceEntities/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Test Entity")));
    }
}
