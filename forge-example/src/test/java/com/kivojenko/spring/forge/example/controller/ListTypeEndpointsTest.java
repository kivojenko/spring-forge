package com.kivojenko.spring.forge.example.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kivojenko.spring.forge.example.WithPostgres;
import com.kivojenko.spring.forge.example.model.sort.SortTestListItem;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;

@SpringBootTest
@AutoConfigureMockMvc
public class ListTypeEndpointsTest extends WithPostgres {

  @BeforeEach
  void setUp() throws Exception {
    for (var title : new String[] {"Alpha", "Charlie", "Beta"}) {
      mockMvc.perform(post("/sortTestListItems")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(SortTestListItem.builder().title(title).build())))
          .andExpect(status().isCreated());
    }
  }

  @AfterEach
  public void cleanUpData() {
    jdbcTemplate.update("DELETE FROM sort_test_list_items");
  }

  @Test
  void returnsPlainListWithDefaultSort() throws Exception {
    mockMvc.perform(get("/sortTestListItems"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(3)))
        .andExpect(jsonPath("$[0].title", is("Charlie")))
        .andExpect(jsonPath("$[1].title", is("Beta")))
        .andExpect(jsonPath("$[2].title", is("Alpha")));
  }

  @Test
  void ignoresPagingAndAppliesExplicitSort() throws Exception {
    mockMvc.perform(get("/sortTestListItems").param("sort", "title,asc").param("size", "1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(3)))
        .andExpect(jsonPath("$[0].title", is("Alpha")))
        .andExpect(jsonPath("$[2].title", is("Charlie")));
  }

  @Test
  void appliesFilterToList() throws Exception {
    mockMvc.perform(get("/sortTestListItems").param("title", "Beta"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].title", is("Beta")));
  }
}
