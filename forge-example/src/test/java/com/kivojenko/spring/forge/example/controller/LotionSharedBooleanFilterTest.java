package com.kivojenko.spring.forge.example.controller;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kivojenko.spring.forge.example.WithPostgres;
import com.kivojenko.spring.forge.example.model.filter.LotionForgeRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;

/**
 * Integration tests for a Boolean parameter mapped to several targets: {@code true} matches rows with any target
 * set, {@code false} rows with none set. Parameters reading a single target, one of them shared with the mapped
 * parameter, keep working next to it.
 */
@SpringBootTest
@AutoConfigureMockMvc
public class LotionSharedBooleanFilterTest extends WithPostgres {

  @Autowired
  private LotionForgeRepository lotionRepository;

  @BeforeEach
  void setUp() throws Exception {
    lotionRepository.deleteAll();

    createLotion("Chemical", true, false);
    createLotion("Mineral", false, true);
    createLotion("Hybrid", true, true);
    createLotion("Plain", false, false);
  }

  @AfterEach
  void tearDown() {
    lotionRepository.deleteAll();
  }

  private void createLotion(String name, boolean chemicalUvFilter, boolean mineralUvFilter) throws Exception {
    var body = "{"
        + "\"name\":\"" + name + "\","
        + "\"chemicalUvFilter\":" + chemicalUvFilter + ","
        + "\"mineralUvFilter\":" + mineralUvFilter
        + "}";

    mockMvc.perform(post("/lotions")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isCreated());
  }

  @Test
  void trueMatchesRowsWithAnyTargetSet() throws Exception {
    mockMvc.perform(get("/lotions").param("uvFilter", "true"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[*].name", containsInAnyOrder("Chemical", "Mineral", "Hybrid")));
  }

  @Test
  void falseMatchesRowsWithNoTargetSet() throws Exception {
    mockMvc.perform(get("/lotions").param("uvFilter", "false"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[*].name", containsInAnyOrder("Plain")));
  }

  @Test
  void singleTargetParametersReadOnlyTheirOwnTarget() throws Exception {
    mockMvc.perform(get("/lotions").param("chemicalUvFilter", "true"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[*].name", containsInAnyOrder("Chemical", "Hybrid")));

    mockMvc.perform(get("/lotions").param("mineralUvFilter", "true"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[*].name", containsInAnyOrder("Mineral", "Hybrid")));
  }

  @Test
  void isAndEdWithOtherFilters() throws Exception {
    mockMvc.perform(get("/lotions").param("uvFilter", "true").param("name", "mineral"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(1)))
        .andExpect(jsonPath("$.content[0].name").value("Mineral"));
  }

  @Test
  void generatesOneFinderPerProperty() {
    lotionRepository.findByChemicalUvFilter(true);
    lotionRepository.findByMineralUvFilter(true);
  }
}
