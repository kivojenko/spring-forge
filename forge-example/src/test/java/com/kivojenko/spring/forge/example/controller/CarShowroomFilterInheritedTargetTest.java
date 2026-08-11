package com.kivojenko.spring.forge.example.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kivojenko.spring.forge.example.WithPostgres;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
public class CarShowroomFilterInheritedTargetTest extends WithPostgres {

  private long createCar(String name, int doors) throws Exception {
    var mvcResult = mockMvc.perform(post("/cars")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{" +
                "\"name\":\"" + name + "\"," +
                "\"numberOfDoors\":" + doors +
                "}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.name", is(name)))
        .andReturn();
    ObjectMapper om = new ObjectMapper();
    JsonNode node = om.readTree(mvcResult.getResponse().getContentAsString());
    return node.get("id").asLong();
  }

  private void createShowroom(String name, long carId) throws Exception {
    mockMvc.perform(post("/carShowrooms")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{" +
                "\"name\":\"" + name + "\"," +
                "\"car\":{\"id\":" + carId + "}" +
                "}"))
        .andExpect(status().isCreated());
  }

  @BeforeEach
  void setUp() throws Exception {
    long focusId = createCar("Focus", 4);
    long camryId = createCar("Camry", 4);

    createShowroom("Focus Hub", focusId);
    createShowroom("Focus Land", focusId);
    createShowroom("Camry Center", camryId);
  }

  @Test
  void shouldFilterShowroomsByCarsInheritedName() throws Exception {
    mockMvc.perform(get("/carShowrooms").param("car", "FOCUS"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(2)))
        .andExpect(jsonPath("$.content[?(@.name == 'Focus Hub')]").exists())
        .andExpect(jsonPath("$.content[?(@.name == 'Focus Land')]").exists());

    mockMvc.perform(get("/carShowrooms").param("car", "camry"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(1)))
        .andExpect(jsonPath("$.content[0].name", is("Camry Center")));
  }
}
