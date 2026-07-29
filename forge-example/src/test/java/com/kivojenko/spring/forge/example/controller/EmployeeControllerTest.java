package com.kivojenko.spring.forge.example.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kivojenko.spring.forge.example.WithPostgres;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
public class EmployeeControllerTest extends WithPostgres {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM employee");

        jdbcTemplate.update("INSERT INTO employee (name, emp_type, team_size) VALUES (?, ?, ?)", "Alice", 1, 5);
        jdbcTemplate.update("INSERT INTO employee (name, emp_type, programming_language) VALUES (?, ?, ?)", "Bob", 2, "Java");
        jdbcTemplate.update("INSERT INTO employee (name, emp_type, programming_language) VALUES (?, ?, ?)", "Charlie", 2, "Kotlin");
    }

    @Test
    void testFilterByEmployeeType() throws Exception {
        mockMvc.perform(get("/employees")
                .param("empType", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].name", is("Alice")));

        mockMvc.perform(get("/employees")
                .param("empType", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)));
    }

    @Test
    void testFilterByEmployeeTypeMultiple() throws Exception {
        mockMvc.perform(get("/employees")
                .param("empType", "1", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(3)));
    }
}
