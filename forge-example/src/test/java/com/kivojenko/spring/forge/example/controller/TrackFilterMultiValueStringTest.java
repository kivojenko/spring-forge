package com.kivojenko.spring.forge.example.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kivojenko.spring.forge.example.WithPostgres;
import com.kivojenko.spring.forge.example.model.filter.TrackForgeRepository;
import java.lang.reflect.Field;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;

/**
 * Integration tests for the multi-value string match modes ({@code *_ANY}): the filter parameter takes
 * several values and a row matches when any of them does.
 */
@SpringBootTest
@AutoConfigureMockMvc
public class TrackFilterMultiValueStringTest extends WithPostgres {

  @Autowired
  private TrackForgeRepository trackRepository;

  @BeforeEach
  void setUp() throws Exception {
    trackRepository.deleteAll();

    createTrack("Alfa", null, "Blue Note", "Ada", null, "jazz", "live");
    createTrack("Beta", null, "Verve", null, "Bea", "classical");
    createTrack("Gamma", "Zeta", null, "Cyd", "Cyd", "jazz-fusion");
    createTrack("Delta", null, "Blue Note Tokyo", "Ada", "Bea");
  }

  @AfterEach
  void tearDown() {
    trackRepository.deleteAll();
  }

  private void createTrack(
      String name, String originalName, String label, String artist, String composer, String... tags)
      throws Exception {
    var body = "{"
        + "\"name\":\"" + name + "\","
        + "\"originalName\":" + json(originalName) + ","
        + "\"label\":" + json(label) + ","
        + "\"artist\":" + json(artist) + ","
        + "\"composer\":" + json(composer) + ","
        + "\"tags\":" + jsonArray(tags)
        + "}";

    mockMvc.perform(post("/tracks")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isCreated());
  }

  private static String json(String value) {
    return value == null ? "null" : "\"" + value + "\"";
  }

  private static String jsonArray(String[] values) {
    var sb = new StringBuilder("[");
    for (int i = 0; i < values.length; i++) {
      if (i > 0) sb.append(',');
      sb.append('"').append(values[i]).append('"');
    }
    return sb.append(']').toString();
  }

  @Test
  void aMultiValueModeExposesACollectionParameter() throws Exception {
    var filterClass = Class.forName("com.kivojenko.spring.forge.example.model.filter.TrackForgeFilter");
    Field name = filterClass.getDeclaredField("name");
    Assertions.assertEquals(Set.class, name.getType(), "A *_ANY mode should expose a Set of values");
  }

  @Test
  void valuesCanAlsoBeSentCommaSeparated() throws Exception {
    // Spring binds a comma-separated parameter to the same set as a repeated one
    mockMvc.perform(get("/tracks").param("name", "alfa,BETA"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(2)))
        .andExpect(jsonPath("$.content[?(@.name == 'Alfa')]").exists())
        .andExpect(jsonPath("$.content[?(@.name == 'Beta')]").exists());
  }

  @Test
  void equalsAnyMatchesAnyOfTheValuesIgnoringCase() throws Exception {
    mockMvc.perform(get("/tracks").param("name", "alfa").param("name", "BETA"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(2)))
        .andExpect(jsonPath("$.content[?(@.name == 'Alfa')]").exists())
        .andExpect(jsonPath("$.content[?(@.name == 'Beta')]").exists());
  }

  @Test
  void aSingleValueBehavesLikeTheSingleValueMode() throws Exception {
    mockMvc.perform(get("/tracks").param("name", "alfa"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(1)))
        .andExpect(jsonPath("$.content[0].name").value("Alfa"));
  }

  @Test
  void blankValuesContributeNothing() throws Exception {
    mockMvc.perform(get("/tracks").param("name", ""))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(4)));

    // A blank value alongside a real one narrows to the real one only
    mockMvc.perform(get("/tracks").param("name", "").param("name", "alfa"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(1)))
        .andExpect(jsonPath("$.content[0].name").value("Alfa"));
  }

  @Test
  void everyValueIsMatchedAgainstEveryTargetSharingTheParameterName() throws Exception {
    // "name" also maps to the originalName column
    mockMvc.perform(get("/tracks").param("name", "zeta"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(1)))
        .andExpect(jsonPath("$.content[0].name").value("Gamma"));

    mockMvc.perform(get("/tracks").param("name", "alfa").param("name", "zeta"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(2)))
        .andExpect(jsonPath("$.content[?(@.name == 'Alfa')]").exists())
        .andExpect(jsonPath("$.content[?(@.name == 'Gamma')]").exists());
  }

  @Test
  void containsAnyIgnoreCaseAlsoKeepsNullsWithOrNull() throws Exception {
    // Verve matches Beta, and Gamma comes along because its label is null
    mockMvc.perform(get("/tracks").param("label", "verve"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(2)))
        .andExpect(jsonPath("$.content[?(@.name == 'Beta')]").exists())
        .andExpect(jsonPath("$.content[?(@.name == 'Gamma')]").exists());

    mockMvc.perform(get("/tracks").param("label", "tokyo").param("label", "VERVE"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(3)))
        .andExpect(jsonPath("$.content[?(@.name == 'Beta')]").exists())
        .andExpect(jsonPath("$.content[?(@.name == 'Delta')]").exists())
        .andExpect(jsonPath("$.content[?(@.name == 'Gamma')]").exists());
  }

  @Test
  void startsWithAnyMatchesTheElementsOfACollection() throws Exception {
    mockMvc.perform(get("/tracks").param("tags", "jazz"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(2)))
        .andExpect(jsonPath("$.content[?(@.name == 'Alfa')]").exists())
        .andExpect(jsonPath("$.content[?(@.name == 'Gamma')]").exists());

    mockMvc.perform(get("/tracks").param("tags", "class").param("tags", "live"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(2)))
        .andExpect(jsonPath("$.content[?(@.name == 'Alfa')]").exists())
        .andExpect(jsonPath("$.content[?(@.name == 'Beta')]").exists());

    // STARTS_WITH_ANY, so a value that only appears inside an element matches nothing
    mockMvc.perform(get("/tracks").param("tags", "fusion"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(0)));
  }

  @Test
  void multiValueMembersOfAFamilyAreOrEdWithEachOther() throws Exception {
    mockMvc.perform(get("/tracks").param("artist", "ada").param("artist", "cyd"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(3)))
        .andExpect(jsonPath("$.content[?(@.name == 'Alfa')]").exists())
        .andExpect(jsonPath("$.content[?(@.name == 'Delta')]").exists())
        .andExpect(jsonPath("$.content[?(@.name == 'Gamma')]").exists());

    // The family OR-s its members, so either credit matching any of its values is enough
    mockMvc.perform(get("/tracks").param("artist", "cyd").param("composer", "bea"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(3)))
        .andExpect(jsonPath("$.content[?(@.name == 'Beta')]").exists())
        .andExpect(jsonPath("$.content[?(@.name == 'Delta')]").exists())
        .andExpect(jsonPath("$.content[?(@.name == 'Gamma')]").exists());
  }

  @Test
  void filtersOutsideAFamilyStillNarrowIt() throws Exception {
    mockMvc.perform(get("/tracks")
            .param("artist", "ada")
            .param("name", "alfa").param("name", "delta"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(2)))
        .andExpect(jsonPath("$.content[?(@.name == 'Alfa')]").exists())
        .andExpect(jsonPath("$.content[?(@.name == 'Delta')]").exists());
  }
}
