package com.kivojenko.spring.forge.example.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kivojenko.spring.forge.example.WithPostgres;
import com.kivojenko.spring.forge.example.model.filter.SunscreenForgeRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;

/**
 * Integration tests for range members of a filter family: a member's min and max are AND-ed into one
 * range first, and that range is then OR-ed with the other members.
 */
@SpringBootTest
@AutoConfigureMockMvc
public class SunscreenFilterFamilyRangeTest extends WithPostgres {

  @Autowired
  private SunscreenForgeRepository sunscreenRepository;

  @BeforeEach
  void setUp() throws Exception {
    sunscreenRepository.deleteAll();

    createSunscreen("Daily", 30, null, null);
    createSunscreen("Beach", 50, null, null);
    createSunscreen("Everyday Range", null, 20, 40);
    createSunscreen("Beach Range", null, 45, 60);
  }

  @AfterEach
  void tearDown() {
    sunscreenRepository.deleteAll();
  }

  private void createSunscreen(String name, Integer spf, Integer minSpf, Integer maxSpf) throws Exception {
    var body = "{"
        + "\"name\":\"" + name + "\","
        + "\"spf\":" + spf + ","
        + "\"minSpf\":" + minSpf + ","
        + "\"maxSpf\":" + maxSpf
        + "}";

    mockMvc.perform(post("/sunscreens")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isCreated());
  }

  @Test
  void aMembersMinAndMaxAreAndEdWithEachOther() throws Exception {
    // Were the bounds OR-ed, spf >= 25 OR spf <= 35 would match both rows holding an spf
    mockMvc.perform(get("/sunscreens")
            .param("minSpf", "25")
            .param("maxSpf", "35"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(1)))
        .andExpect(jsonPath("$.content[0].name").value("Daily"));
  }

  @Test
  void aSingleBoundFiltersOnItsOwn() throws Exception {
    mockMvc.perform(get("/sunscreens").param("minSpf", "40"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(1)))
        .andExpect(jsonPath("$.content[0].name").value("Beach"));

    mockMvc.perform(get("/sunscreens").param("maxMaxSpf", "40"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(1)))
        .andExpect(jsonPath("$.content[0].name").value("Everyday Range"));
  }

  @Test
  void rangesAreOrEdWithEachOther() throws Exception {
    // (spf in 25..35) OR (minSpf in 40..50) OR (maxSpf in 35..45)
    mockMvc.perform(get("/sunscreens")
            .param("minSpf", "25")
            .param("maxSpf", "35")
            .param("minMinSpf", "40")
            .param("maxMinSpf", "50")
            .param("minMaxSpf", "35")
            .param("maxMaxSpf", "45"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(3)))
        .andExpect(jsonPath("$.content[?(@.name == 'Daily')]").exists())
        .andExpect(jsonPath("$.content[?(@.name == 'Beach Range')]").exists())
        .andExpect(jsonPath("$.content[?(@.name == 'Everyday Range')]").exists());
  }

  @Test
  void anExactValueIsOrEdWithTheRange() throws Exception {
    // spf = 50 OR (spf in 25..35)
    mockMvc.perform(get("/sunscreens")
            .param("spf", "50")
            .param("minSpf", "25")
            .param("maxSpf", "35"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(2)))
        .andExpect(jsonPath("$.content[?(@.name == 'Daily')]").exists())
        .andExpect(jsonPath("$.content[?(@.name == 'Beach')]").exists());
  }

  @Test
  void theFamilyIsAndEdWithFiltersOutsideIt() throws Exception {
    mockMvc.perform(get("/sunscreens")
            .param("name", "beach")
            .param("minSpf", "25")
            .param("maxSpf", "35")
            .param("minMinSpf", "40")
            .param("maxMinSpf", "50"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(1)))
        .andExpect(jsonPath("$.content[0].name").value("Beach Range"));
  }

  @Test
  void anEmptyFamilyLeavesTheResultsUntouched() throws Exception {
    mockMvc.perform(get("/sunscreens"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(4)));
  }
}
