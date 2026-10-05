package com.kivojenko.spring.forge.example.controller;

import com.kivojenko.spring.forge.example.WithPostgres;
import com.kivojenko.spring.forge.example.model.view.ViewTestChild;
import com.kivojenko.spring.forge.example.model.view.ViewTestEntity;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
public class ViewEndpointsTest extends WithPostgres {

  @Test
  void testWithEndpointsAndWithGetEndpointView() throws Exception {
    String parentJson = mockMvc
        .perform(post("/viewTestEntities")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(ViewTestEntity.builder()
                .name("Parent Entity")
                .secretNote("Secret note on parent")
                .build())))
        .andExpect(status().isCreated())
        .andReturn()
        .getResponse()
        .getContentAsString();

    Long parentId = objectMapper.readTree(parentJson).get("id").asLong();

    mockMvc
        .perform(post("/viewTestEntities/{id}/children", parentId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(ViewTestChild.builder()
                .title("Child Title")
                .hiddenDetail("Very sensitive child detail")
                .build())))
        .andExpect(status().isCreated());

    // Test @WithEndpoints(view = Views.Summary.class) on GET /viewTestEntities/{id}/children
    // It should include fields marked with Views.Summary (id, title) and exclude fields not marked with Views.Summary (hiddenDetail)
    mockMvc
        .perform(get("/viewTestEntities/{id}/children", parentId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].title", is("Child Title")))
        .andExpect(jsonPath("$[0].hiddenDetail").doesNotExist());

    // Test @WithGetEndpoint(view = Views.Summary.class) on GET /viewTestEntities/{id}/specialChildren
    mockMvc
        .perform(get("/viewTestEntities/{id}/specialChildren", parentId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].title", is("Child Title")))
        .andExpect(jsonPath("$[0].hiddenDetail").doesNotExist());
  }
}
