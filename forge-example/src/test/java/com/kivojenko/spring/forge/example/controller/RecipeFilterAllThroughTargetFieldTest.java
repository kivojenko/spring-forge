package com.kivojenko.spring.forge.example.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kivojenko.spring.forge.example.WithPostgres;
import com.kivojenko.spring.forge.example.model.filter.Recipe;
import com.kivojenko.spring.forge.example.model.filter.RecipeForgeFilter;
import com.kivojenko.spring.forge.example.model.filter.RecipeForgeRepository;
import com.kivojenko.spring.forge.example.model.filter.RecipeSpice;
import com.kivojenko.spring.forge.example.model.filter.RecipeSpiceForgeRepository;
import com.kivojenko.spring.forge.example.model.filter.Spice;
import com.kivojenko.spring.forge.example.model.filter.SpiceForgeRepository;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

/**
 * Integration tests for {@code IterableMatchMode.ALL} through a {@code targetField} of an entity collection
 * ({@code recipe.recipeSpices.any().spice.id}): the parameter takes several values and a recipe matches when every
 * one of them is found on some element, even when the values sit on different elements.
 */
@SpringBootTest
@AutoConfigureMockMvc
public class RecipeFilterAllThroughTargetFieldTest extends WithPostgres {

  @Autowired
  private RecipeForgeRepository recipeRepository;

  @Autowired
  private RecipeSpiceForgeRepository recipeSpiceRepository;

  @Autowired
  private SpiceForgeRepository spiceRepository;

  private Spice cumin;
  private Spice chili;
  private Spice saffron;

  @BeforeEach
  void setUp() {
    cleanUpRecipes();
    cumin = spiceRepository.save(Spice.builder().name("Cumin").build());
    chili = spiceRepository.save(Spice.builder().name("Chili").build());
    saffron = spiceRepository.save(Spice.builder().name("Saffron").build());

    createRecipe("Curry", cumin, chili);
    createRecipe("Paella", saffron);
    createRecipe("Chili con carne", chili);
  }

  @AfterEach
  void cleanUpRecipes() {
    recipeSpiceRepository.deleteAll();
    recipeRepository.deleteAll();
    spiceRepository.deleteAll();
  }

  private void createRecipe(String name, Spice... spices) {
    var recipe = recipeRepository.save(Recipe.builder().name(name).build());
    for (var spice : spices) {
      recipeSpiceRepository.save(RecipeSpice.builder().recipe(recipe).spice(spice).build());
    }
  }

  @Test
  void shouldExposeASetParameterForAllAndKeepTheSingleValueDefault() throws Exception {
    assertEquals(Set.class, RecipeForgeFilter.class.getDeclaredField("spices").getType());
    assertEquals(Set.class, RecipeForgeFilter.class.getDeclaredField("spiceNames").getType());
    assertEquals(Set.class, RecipeForgeFilter.class.getDeclaredField("anySpiceNames").getType());
    assertEquals(String.class, RecipeForgeFilter.class.getDeclaredField("spiceName").getType());
  }

  @Test
  void shouldMatchAllIds_spreadAcrossElements() throws Exception {
    // Curry has cumin and chili on two different RecipeSpice rows
    mockMvc.perform(get("/recipes").param("spices", cumin.getId() + "," + chili.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(1)))
        .andExpect(jsonPath("$.content[0].name").value("Curry"));

    // Repeated parameters bind the same way
    mockMvc.perform(get("/recipes")
            .param("spices", String.valueOf(cumin.getId()))
            .param("spices", String.valueOf(chili.getId())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(1)))
        .andExpect(jsonPath("$.content[0].name").value("Curry"));

    // A single id matches every recipe containing it
    mockMvc.perform(get("/recipes").param("spices", String.valueOf(chili.getId())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(2)))
        .andExpect(jsonPath("$.content[?(@.name == 'Curry')]").exists())
        .andExpect(jsonPath("$.content[?(@.name == 'Chili con carne')]").exists());
  }

  @Test
  void shouldNotMatchWhenOneOfTheIdsIsMissing() throws Exception {
    mockMvc.perform(get("/recipes").param("spices", cumin.getId() + "," + saffron.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(0)));
  }

  @Test
  void shouldMatchAllStringTargets_exactly() throws Exception {
    mockMvc.perform(get("/recipes").param("spiceNames", "Cumin", "Chili"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(1)))
        .andExpect(jsonPath("$.content[0].name").value("Curry"));

    // ALL compares each value exactly, not with the default CONTAINS
    mockMvc.perform(get("/recipes").param("spiceNames", "Cum"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(0)));
  }

  @Test
  void shouldMatchAnyOfSeveralValues_withAMultiValueStringMode() throws Exception {
    mockMvc.perform(get("/recipes").param("anySpiceNames", "Cumin", "Saffron"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(2)))
        .andExpect(jsonPath("$.content[?(@.name == 'Curry')]").exists())
        .andExpect(jsonPath("$.content[?(@.name == 'Paella')]").exists());
  }

  @Test
  void shouldKeepTheSingleValueDefault() throws Exception {
    mockMvc.perform(get("/recipes").param("spiceName", "affr"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(1)))
        .andExpect(jsonPath("$.content[0].name").value("Paella"));
  }
}
