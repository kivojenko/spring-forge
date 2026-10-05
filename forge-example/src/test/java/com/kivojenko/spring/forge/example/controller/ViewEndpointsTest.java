package com.kivojenko.spring.forge.example.controller;

import com.kivojenko.spring.forge.example.WithPostgres;
import com.kivojenko.spring.forge.example.model.view.SplitViewTestEntity;
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

    // Test @WithRestController(view = Views.Summary.class) on GET /viewTestEntities/{id}
    // It should include fields marked with Views.Summary (id, name) and exclude fields not marked with Views.Summary (secretNote)
    mockMvc
        .perform(get("/viewTestEntities/{id}", parentId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name", is("Parent Entity")))
        .andExpect(jsonPath("$.secretNote").doesNotExist());

    // Test @WithRestController(view = Views.Summary.class) on GET /viewTestEntities (findAll)
    mockMvc
        .perform(get("/viewTestEntities"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].name", is("Parent Entity")))
        .andExpect(jsonPath("$.content[0].secretNote").doesNotExist());

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

  @Test
  void testSplitListViewAndDetailView() throws Exception {
    String entityJson = mockMvc
        .perform(post("/splitViewTestEntities")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(SplitViewTestEntity.builder()
                .name("Split Entity")
                .secretNote("Super secret detail")
                .build())))
        .andExpect(status().isCreated())
        .andReturn()
        .getResponse()
        .getContentAsString();

    Long entityId = objectMapper.readTree(entityJson).get("id").asLong();

    // GET /splitViewTestEntities (findAll - listView = Views.Summary.class)
    // should include 'name' and exclude 'secretNote'
    mockMvc
        .perform(get("/splitViewTestEntities"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].name", is("Split Entity")))
        .andExpect(jsonPath("$.content[0].secretNote").doesNotExist());

    // GET /splitViewTestEntities/{id} (getById - detailView = Views.Detail.class)
    // should include both 'name' and 'secretNote'
    mockMvc
        .perform(get("/splitViewTestEntities/{id}", entityId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name", is("Split Entity")))
        .andExpect(jsonPath("$.secretNote", is("Super secret detail")));
  }
}
