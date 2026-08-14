package com.kivojenko.spring.forge.example.controller;

import com.kivojenko.spring.forge.example.WithPostgres;
import com.kivojenko.spring.forge.example.model.inheritance.Car;
import com.kivojenko.spring.forge.example.model.inheritance.Truck;
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
class VehicleControllerTest extends WithPostgres {

    @BeforeEach
    void setUp() throws Exception {
        createCar("Tesla Model 3", 4);
        createCar("Mini Cooper", 2);
        createTruck("Ford F-150", 1000.0);
    }

    private void createCar(String name, int doors) throws Exception {
        mockMvc.perform(post("/cars")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new Car() {{
                    setName(name);
                    setNumberOfDoors(doors);
                }})))
                .andExpect(status().isCreated());
    }

    private void createTruck(String name, double capacity) throws Exception {
        mockMvc.perform(post("/trucks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new Truck() {{
                    setName(name);
                    setPayloadCapacity(capacity);
                }})))
                .andExpect(status().isCreated());
    }

    @Test
    void testFindAllVehicles() throws Exception {
        mockMvc.perform(get("/vehicles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(3)));
    }

    @Test
    void testFilterByVehicleTypeCar() throws Exception {
        mockMvc.perform(get("/vehicles")
                .param("vehicleType", "CAR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].name", is("Tesla Model 3")))
                .andExpect(jsonPath("$.content[1].name", is("Mini Cooper")));
    }

    @Test
    void testFilterByVehicleTypeTruck() throws Exception {
        mockMvc.perform(get("/vehicles")
                .param("vehicleType", "TRUCK"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].name", is("Ford F-150")));
    }

    @Test
    void testFilterByVehicleTypeMultiple() throws Exception {
        mockMvc.perform(get("/vehicles")
                .param("vehicleType", "CAR", "TRUCK"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(3)));
    }

    @Test
    void testPostVehicleNotAllowed() throws Exception {
        mockMvc.perform(post("/vehicles")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"Abstract Vehicle\"}"))
                .andExpect(status().isMethodNotAllowed());
    }
}
