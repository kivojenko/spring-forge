package com.kivojenko.spring.forge.example.controller;

import com.kivojenko.spring.forge.example.WithPostgres;
import com.kivojenko.spring.forge.example.model.collection.Cas;
import com.kivojenko.spring.forge.example.model.collection.CasForgeRepository;
import com.kivojenko.spring.forge.example.model.collection.Chemical;
import com.kivojenko.spring.forge.example.model.collection.ChemicalForgeRepository;
import com.kivojenko.spring.forge.example.model.collection.ChemicalForgeService;
import com.kivojenko.spring.forge.example.model.collection.HazardForgeRepository;
import com.kivojenko.spring.forge.example.model.collection.NumberReferences;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
public class ChemicalCollectionGetOrCreateTest extends WithPostgres {

  private static final String CAS_PARAM = "numberReferences.casNumbers.number";

  @Autowired
  private ChemicalForgeService chemicalService;

  @Autowired
  private ChemicalForgeRepository chemicalRepository;

  @Autowired
  private CasForgeRepository casRepository;

  @Autowired
  private HazardForgeRepository hazardRepository;

  @BeforeEach
  void cleanChemicals() {
    chemicalRepository.deleteAll();
    casRepository.deleteAll();
    hazardRepository.deleteAll();
  }

  @Test
  void testGetOrCreateByCasNumberInsideEmbedded() throws Exception {
    // Creates the chemical, its embedded references and the missing CAS entity
    mockMvc
        .perform(post("/chemicals/get-or-create").param(CAS_PARAM, "7732-18-5"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.numberReferences.casNumbers[0].number", is("7732-18-5")))
        .andExpect(jsonPath("$.id").exists());

    assertTrue(casRepository.existsById("7732-18-5"));

    mockMvc
        .perform(post("/chemicals/get-or-create").param(CAS_PARAM, "7732-18-5"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.numberReferences.casNumbers[0].number", is("7732-18-5")));

    mockMvc.perform(get("/chemicals/count")).andExpect(status().isOk()).andExpect(jsonPath("$", is(1)));
    assertEquals(1, casRepository.count());
  }

  @Test
  void testGetOrCreateByCasNumberReusesExistingCas() {
    casRepository.save(Cas.builder().number("64-17-5").build());

    var created = chemicalService.getOrCreateByNumberReferencesCasNumbersNumber("64-17-5");

    assertEquals(1, casRepository.count());
    assertEquals(created.getId(), chemicalService.getOrCreateByNumberReferencesCasNumbersNumber("64-17-5").getId());
  }

  @Test
  void testGetOrCreateMatchesAnyElement() {
    var first = casRepository.save(Cas.builder().number("50-00-0").build());
    var second = casRepository.save(Cas.builder().number("30525-89-4").build());
    var chemical = chemicalRepository.save(Chemical.builder()
        .name("Formaldehyde")
        .numberReferences(NumberReferences.builder().casNumbers(new HashSet<>(Set.of(first, second))).build())
        .synonyms(new HashSet<>(Set.of("Methanal", "Formalin")))
        .build());

    assertEquals(chemical.getId(), chemicalService.getOrCreateByNumberReferencesCasNumbersNumber("30525-89-4").getId());
    assertEquals(chemical.getId(), chemicalService.getOrCreateByNumberReferencesCasNumbersNumber("50-00-0").getId());
    assertEquals(chemical.getId(), chemicalService.getOrCreateBySynonyms("formalin").getId());
    assertEquals(1, chemicalRepository.count());
  }

  @Test
  void testGetOrCreateBySynonymElementCollection() throws Exception {
    mockMvc
        .perform(post("/chemicals/get-or-create").param("synonyms", "Aqua"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.synonyms", containsInAnyOrder("Aqua")));

    // ignoreCase applies to the elements
    mockMvc
        .perform(post("/chemicals/get-or-create").param("synonyms", "AQUA"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.synonyms", containsInAnyOrder("Aqua")));

    mockMvc.perform(get("/chemicals/count")).andExpect(status().isOk()).andExpect(jsonPath("$", is(1)));

    mockMvc
        .perform(post("/chemicals/get-or-create").param("synonyms", "Water"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.synonyms", containsInAnyOrder("Water")));

    mockMvc.perform(get("/chemicals/count")).andExpect(status().isOk()).andExpect(jsonPath("$", is(2)));
  }

  @Test
  void testSeveralMatchesReturnTheLowestId() {
    var older = chemicalRepository.save(Chemical.builder().name("Ethanol").synonyms(new HashSet<>(Set.of("Spirit"))).build());
    chemicalRepository.save(Chemical.builder().name("Methanol").synonyms(new HashSet<>(Set.of("Spirit"))).build());

    assertEquals(older.getId(), chemicalService.getOrCreateBySynonyms("spirit").getId());
    assertEquals(2, chemicalRepository.count());
  }

  @Test
  void testGetOrCreateByNameAndHazardCode() throws Exception {
    mockMvc
        .perform(post("/chemicals/get-or-create").param("name", "Ethanol").param("hazards.code", "H225"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name", is("Ethanol")))
        .andExpect(jsonPath("$.hazards[0].code", is("H225")));

    // Both parts of the key are case-insensitive
    mockMvc
        .perform(post("/chemicals/get-or-create").param("name", "ETHANOL").param("hazards.code", "h225"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name", is("Ethanol")));

    mockMvc.perform(get("/chemicals/count")).andExpect(status().isOk()).andExpect(jsonPath("$", is(1)));

    // Same name, other hazard: a new chemical
    mockMvc
        .perform(post("/chemicals/get-or-create").param("name", "Ethanol").param("hazards.code", "H319"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.hazards[0].code", is("H319")));

    mockMvc.perform(get("/chemicals/count")).andExpect(status().isOk()).andExpect(jsonPath("$", is(2)));
    assertEquals(2, hazardRepository.count());
  }

  @Test
  void testServiceMethodsAreNamedAfterTheKeys() {
    var byCas = chemicalService.getOrCreateByNumberReferencesCasNumbersNumber("67-56-1");
    var bySynonym = chemicalService.getOrCreateBySynonyms("Wood alcohol");
    var byNameAndHazard = chemicalService.getOrCreateByNameAndHazardsCode("Methanol", "H301");

    assertNotEquals(byCas.getId(), bySynonym.getId());
    assertNotEquals(bySynonym.getId(), byNameAndHazard.getId());
  }
}
