package com.kivojenko.spring.forge.example.controller;

import com.kivojenko.spring.forge.example.WithPostgres;
import com.kivojenko.spring.forge.example.model.filter.Tag;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class TagControllerTest extends WithPostgres {

    @BeforeEach
    void setUp() throws Exception {
        createTag("Sale");
        createTag("New");
        createTag("Featured");
    }

    private void createTag(String name) throws Exception {
        mockMvc.perform(post("/tags")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Tag.builder().name(name).build())))
                .andExpect(status().isCreated());
    }

    @Test
    void testFindAllWithFilter() throws Exception {
        mockMvc.perform(get("/tags")
                .param("name", "Sale"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].name", is("Sale")));
    }

    @Test
    void testFindAllWithRequiredFieldMissing() throws Exception {
        // name field is required, so missing it should result in 400 Bad Request
        mockMvc.perform(get("/tags"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testFindAllWithRequiredFieldBlank() throws Exception {
        // name field is required and marked as @NotBlank, so blank string should result in 400 Bad Request
        mockMvc.perform(get("/tags")
                .param("name", "   "))
                .andExpect(status().isBadRequest());
    }
}
