package com.kivojenko.spring.forge.example.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kivojenko.spring.forge.example.WithPostgres;
import com.kivojenko.spring.forge.example.model.filter.Country;
import com.kivojenko.spring.forge.example.model.filter.Office;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;

@SpringBootTest
@AutoConfigureMockMvc
public class OfficeControllerTest extends WithPostgres {

    @BeforeEach
    void setUp() throws Exception {
        createCountry("US", "United States");
        createCountry("EE", "Estonia");
        createCountry("DE", "Germany");

        createOffice("New York Office", "US");
        createOffice("Tallinn Office", "EE");
        createOffice("Berlin Office", "DE");
        createOffice("Munich Office", "DE");
    }

    private void createCountry(String code, String name) throws Exception {
        mockMvc.perform(post("/countries")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Country.builder()
                        .code(code)
                        .name(name)
                        .build())))
                .andExpect(status().isCreated());
    }

    private void createOffice(String name, String countryCode) throws Exception {
        mockMvc.perform(post("/offices")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Office.builder()
                        .name(name)
                        .country(Country.builder().code(countryCode).build())
                        .build())))
                .andExpect(status().isCreated());
    }

    @Test
    void shouldFilterOfficesByCountryCodes() throws Exception {
        // Filter by a single country code
        mockMvc.perform(get("/offices").param("countries", "EE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].name", is("Tallinn Office")));

        // Filter by multiple country codes
        mockMvc.perform(get("/offices").param("countries", "EE,DE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(3)))
                .andExpect(jsonPath("$.content[?(@.name == 'Tallinn Office')]").exists())
                .andExpect(jsonPath("$.content[?(@.name == 'Berlin Office')]").exists())
                .andExpect(jsonPath("$.content[?(@.name == 'Munich Office')]").exists());
        
        // Filter by non-existent country code
        mockMvc.perform(get("/offices").param("countries", "FR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)));
    }
}
