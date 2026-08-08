package com.kivojenko.spring.forge.example.controller;

import com.kivojenko.spring.forge.example.WithPostgres;
import com.kivojenko.spring.forge.example.model.inheritance.Car;
import com.kivojenko.spring.forge.example.model.inheritance.Truck;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class VehicleFilterTest extends WithPostgres {

    @BeforeEach
    void setUp() throws Exception {
        createCar("Tesla Model S", 4);
        createCar("BMW M3", 4);
        createTruck("Volvo FH", 20.0);
    }

    private void createCar(String name, Integer doors) throws Exception {
        Car car = new Car();
        car.setName(name);
        car.setNumberOfDoors(doors);
        mockMvc.perform(post("/cars")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(car)))
                .andExpect(status().isCreated());
    }

    private void createTruck(String name, Double payload) throws Exception {
        Truck truck = new Truck();
        truck.setName(name);
        truck.setPayloadCapacity(payload);
        mockMvc.perform(post("/trucks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(truck)))
                .andExpect(status().isCreated());
    }

    @Test
    void testFilterCarByName() throws Exception {
        mockMvc.perform(get("/cars").param("name", "Tesla Model S"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].name", is("Tesla Model S")));
    }

    @Test
    void testFilterTruckByName() throws Exception {
        mockMvc.perform(get("/trucks").param("name", "Volvo FH"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].name", is("Volvo FH")));
    }
}
