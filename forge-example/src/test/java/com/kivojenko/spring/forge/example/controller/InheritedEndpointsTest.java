package com.kivojenko.spring.forge.example.controller;

import com.kivojenko.spring.forge.example.WithPostgres;
import com.kivojenko.spring.forge.example.model.inheritance.Car;
import com.kivojenko.spring.forge.example.model.inheritance.MaintenanceRecord;
import com.kivojenko.spring.forge.example.model.inheritance.Truck;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
public class InheritedEndpointsTest extends WithPostgres {

    @Test
    void testCarInheritedEndpoints() throws Exception {
        // Create Car
        String carJson = mockMvc.perform(post("/cars")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new Car() {{
                    setName("BMW 330i");
                    setNumberOfDoors(4);
                }})))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long carId = objectMapper.readTree(carJson).get("id").asLong();

        // Add new maintenance record via inherited POST /cars/{id}/maintenanceRecords
        mockMvc.perform(post("/cars/{id}/maintenanceRecords", carId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(MaintenanceRecord.builder()
                        .description("Oil Change")
                        .build())))
                .andExpect(status().isCreated());

        // Read maintenance records via inherited GET /cars/{id}/maintenanceRecords
        String recordsJson = mockMvc.perform(get("/cars/{id}/maintenanceRecords", carId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].description", is("Oil Change")))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long recordId = objectMapper.readTree(recordsJson).get(0).get("id").asLong();

        // Delete maintenance record via inherited DELETE /cars/{id}/maintenanceRecords/{recordId}
        mockMvc.perform(delete("/cars/{id}/maintenanceRecords/{recordId}", carId, recordId))
                .andExpect(status().isNoContent());

        // Verify empty
        mockMvc.perform(get("/cars/{id}/maintenanceRecords", carId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void testTruckInheritedEndpoints() throws Exception {
        // Create Truck
        String truckJson = mockMvc.perform(post("/trucks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new Truck() {{
                    setName("Volvo FH16");
                    setPayloadCapacity(25000.0);
                }})))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long truckId = objectMapper.readTree(truckJson).get("id").asLong();

        // Add new maintenance record via inherited POST /trucks/{id}/maintenanceRecords
        mockMvc.perform(post("/trucks/{id}/maintenanceRecords", truckId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(MaintenanceRecord.builder()
                        .description("Brake Inspection")
                        .build())))
                .andExpect(status().isCreated());

        // Read maintenance records via inherited GET /trucks/{id}/maintenanceRecords
        mockMvc.perform(get("/trucks/{id}/maintenanceRecords", truckId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].description", is("Brake Inspection")));
    }
}
