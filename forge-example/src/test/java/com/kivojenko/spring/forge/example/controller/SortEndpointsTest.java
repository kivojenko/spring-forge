package com.kivojenko.spring.forge.example.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kivojenko.spring.forge.example.WithPostgres;
import com.kivojenko.spring.forge.example.model.sort.SortTestChild;
import com.kivojenko.spring.forge.example.model.sort.SortTestParent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;

@SpringBootTest
@AutoConfigureMockMvc
public class SortEndpointsTest extends WithPostgres {

  private Long parentId;

  @BeforeEach
  void setUp() throws Exception {
    createParent(SortTestParent.builder().title("Alpha").build());
    createParent(SortTestParent.builder().title("Charlie").build());
    String parentJson = createParent(SortTestParent.builder().title("Beta").build());
    parentId = objectMapper.readTree(parentJson).get("id").asLong();

    mockMvc.perform(post("/sortTestParents/{id}/children", parentId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(SortTestChild.builder().name("Alice").score(50).build())))
        .andExpect(status().isCreated());

    mockMvc.perform(post("/sortTestParents/{id}/children", parentId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(SortTestChild.builder().name("Charlie").score(10).build())))
        .andExpect(status().isCreated());

    mockMvc.perform(post("/sortTestParents/{id}/children", parentId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(SortTestChild.builder().name("Bob").score(100).build())))
        .andExpect(status().isCreated());
  }

  @AfterEach
  public void cleanUpData() throws Exception {
    var parentIds = jdbcTemplate.queryForList("SELECT id FROM sort_test_parents", Long.class);
    for (Long id : parentIds) {
      mockMvc.perform(delete("/sortTestParents/{id}", id)).andExpect(status().isNoContent());
    }
  }

  private String createParent(SortTestParent parent) throws Exception {
    return mockMvc.perform(post("/sortTestParents")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(parent)))
        .andExpect(status().isCreated())
        .andReturn()
        .getResponse()
        .getContentAsString();
  }

  @Test
  void testRestControllerDefaultSort() throws Exception {
    mockMvc.perform(get("/sortTestParents"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(3)))
        .andExpect(jsonPath("$.content[0].title", is("Charlie")))
        .andExpect(jsonPath("$.content[1].title", is("Beta")))
        .andExpect(jsonPath("$.content[2].title", is("Alpha")));
  }

  @Test
  void testRestControllerExplicitSort() throws Exception {
    mockMvc.perform(get("/sortTestParents").param("sort", "title,asc"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(3)))
        .andExpect(jsonPath("$.content[0].title", is("Alpha")))
        .andExpect(jsonPath("$.content[1].title", is("Beta")))
        .andExpect(jsonPath("$.content[2].title", is("Charlie")));
  }

  @Test
  void testWithEndpointsDefaultSort() throws Exception {
    mockMvc.perform(get("/sortTestParents/{id}/children", parentId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(3)))
        .andExpect(jsonPath("$[0].name", is("Charlie")))
        .andExpect(jsonPath("$[1].name", is("Bob")))
        .andExpect(jsonPath("$[2].name", is("Alice")));
  }

  @Test
  void testWithEndpointsExplicitSortAsc() throws Exception {
    mockMvc.perform(get("/sortTestParents/{id}/children", parentId).param("sort", "name,asc"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(3)))
        .andExpect(jsonPath("$[0].name", is("Alice")))
        .andExpect(jsonPath("$[1].name", is("Bob")))
        .andExpect(jsonPath("$[2].name", is("Charlie")));
  }

  @Test
  void testWithEndpointsExplicitSortByScore() throws Exception {
    mockMvc.perform(get("/sortTestParents/{id}/children", parentId).param("sort", "score,asc"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(3)))
        .andExpect(jsonPath("$[0].name", is("Charlie")))
        .andExpect(jsonPath("$[0].score", is(10)))
        .andExpect(jsonPath("$[1].name", is("Alice")))
        .andExpect(jsonPath("$[1].score", is(50)))
        .andExpect(jsonPath("$[2].name", is("Bob")))
        .andExpect(jsonPath("$[2].score", is(100)));
  }

  @Test
  void testWithGetEndpointDefaultSort() throws Exception {
    mockMvc.perform(get("/sortTestParents/{id}/topScoredChildren", parentId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(3)))
        .andExpect(jsonPath("$[0].name", is("Bob")))
        .andExpect(jsonPath("$[0].score", is(100)))
        .andExpect(jsonPath("$[1].name", is("Alice")))
        .andExpect(jsonPath("$[1].score", is(50)))
        .andExpect(jsonPath("$[2].name", is("Charlie")))
        .andExpect(jsonPath("$[2].score", is(10)));
  }

  @Test
  void testWithGetEndpointExplicitSortAsc() throws Exception {
    mockMvc.perform(get("/sortTestParents/{id}/topScoredChildren", parentId).param("sort", "score,asc"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(3)))
        .andExpect(jsonPath("$[0].name", is("Charlie")))
        .andExpect(jsonPath("$[0].score", is(10)))
        .andExpect(jsonPath("$[1].name", is("Alice")))
        .andExpect(jsonPath("$[1].score", is(50)))
        .andExpect(jsonPath("$[2].name", is("Bob")))
        .andExpect(jsonPath("$[2].score", is(100)));
  }

  @Test
  void testWithGetEndpointExplicitSortByName() throws Exception {
    mockMvc.perform(get("/sortTestParents/{id}/topScoredChildren", parentId).param("sort", "name,asc"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(3)))
        .andExpect(jsonPath("$[0].name", is("Alice")))
        .andExpect(jsonPath("$[1].name", is("Bob")))
        .andExpect(jsonPath("$[2].name", is("Charlie")));
  }
}
