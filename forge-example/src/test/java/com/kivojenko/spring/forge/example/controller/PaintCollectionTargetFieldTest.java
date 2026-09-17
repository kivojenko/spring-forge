package com.kivojenko.spring.forge.example.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kivojenko.spring.forge.example.WithPostgres;
import com.kivojenko.spring.forge.example.model.filter.PaintForgeRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;

/**
 * Integration tests for {@code @FilterField} targets that are collections: a {@code targetField}
 * pointing at an {@code @ElementCollection} inside an {@code @Embedded} value object
 * ({@code dye.hexColors}), and an {@code @ElementCollection} of scalars filtered directly
 * ({@code keywords}). Both are matched element-wise through QueryDSL's {@code any()}.
 */
@SpringBootTest
@AutoConfigureMockMvc
public class PaintCollectionTargetFieldTest extends WithPostgres {

  @Autowired
  private PaintForgeRepository paintRepository;

  @BeforeEach
  void setUp() throws Exception {
    paintRepository.deleteAll();

    createPaint("Sunset", 1L, new String[] {"#FF0000", "#FFA500"}, new String[] {"warm", "bright"});
    createPaint("Ocean", 2L, new String[] {"#0000FF", "#00FFFF"}, new String[] {"cool", "deep"});
    createPaint("Plain", 3L, new String[] {}, new String[] {});
  }

  @AfterEach
  void tearDown() {
    paintRepository.deleteAll();
  }

  private void createPaint(String name, Long colorIndex, String[] hexColors, String[] keywords) throws Exception {
    String body = "{"
        + "\"name\":\"" + name + "\","
        + "\"dye\":{\"colorIndex\":" + colorIndex + ",\"hexColors\":" + jsonArray(hexColors) + "},"
        + "\"keywords\":" + jsonArray(keywords)
        + "}";

    mockMvc.perform(post("/paints")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isCreated());
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
  void shouldFilterByCollectionInsideEmbedded_viaTargetField() throws Exception {
    mockMvc.perform(get("/paints").param("hexColor", "#FF0000"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(1)))
        .andExpect(jsonPath("$.content[0].name").value("Sunset"));

    // EQUALS_IGNORE_CASE applies to each element of the collection
    mockMvc.perform(get("/paints").param("hexColor", "#00ffff"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(1)))
        .andExpect(jsonPath("$.content[0].name").value("Ocean"));

    mockMvc.perform(get("/paints").param("hexColor", "#123456"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(0)));
  }

  @Test
  void shouldFilterByCollectionTarget_underTheDefaultParameterName() throws Exception {
    // The reported case: @FilterField(targetField = "hexColors") with no explicit name, so the
    // parameter keeps the annotated field's name. STARTS_WITH + orNull on the same declaration.
    mockMvc.perform(get("/paints").param("dye", "#00"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(2)))
        .andExpect(jsonPath("$.content[?(@.name == 'Ocean')]").exists())
        // orNull: paints with no colours at all are matched too
        .andExpect(jsonPath("$.content[?(@.name == 'Plain')]").exists());

    mockMvc.perform(get("/paints").param("dye", "#FFA"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(2)))
        .andExpect(jsonPath("$.content[?(@.name == 'Sunset')]").exists())
        .andExpect(jsonPath("$.content[?(@.name == 'Plain')]").exists());
  }

  @Test
  void shouldFilterByPresenceOfCollectionInsideEmbedded() throws Exception {
    mockMvc.perform(get("/paints").param("hasHexColors", "true"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(2)))
        .andExpect(jsonPath("$.content[?(@.name == 'Sunset')]").exists())
        .andExpect(jsonPath("$.content[?(@.name == 'Ocean')]").exists());

    mockMvc.perform(get("/paints").param("hasHexColors", "false"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(1)))
        .andExpect(jsonPath("$.content[0].name").value("Plain"));
  }

  @Test
  void shouldStillFilterByScalarInsideTheSameEmbedded() throws Exception {
    mockMvc.perform(get("/paints").param("colorIndex", "2"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(1)))
        .andExpect(jsonPath("$.content[0].name").value("Ocean"));

    mockMvc.perform(get("/paints").param("minColorIndex", "2"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(2)));
  }

  @Test
  void shouldFilterByElementCollectionOfScalars() throws Exception {
    mockMvc.perform(get("/paints").param("keywords", "WAR"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(1)))
        .andExpect(jsonPath("$.content[0].name").value("Sunset"));

    mockMvc.perform(get("/paints").param("keywords", "deep"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(1)))
        .andExpect(jsonPath("$.content[0].name").value("Ocean"));

    mockMvc.perform(get("/paints").param("keywords", "missing"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(0)));
  }

  @Test
  void shouldExposeDerivedQueryForTheScalarTargetOnly() {
    var methods = java.util.Arrays.stream(PaintForgeRepository.class.getDeclaredMethods())
        .map(java.lang.reflect.Method::getName)
        .toList();

    // The embedded scalar is addressed by its real property path…
    assertTrue(methods.contains("findByDye_ColorIndex"), methods.toString());
    // …while collection targets cannot be a SIMPLE_PROPERTY derived query and are left out.
    assertFalse(methods.contains("findByDye"), methods.toString());
    assertFalse(methods.contains("findByKeywords"), methods.toString());
  }
}
