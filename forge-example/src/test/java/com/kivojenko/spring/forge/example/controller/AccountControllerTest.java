package com.kivojenko.spring.forge.example.controller;

import com.kivojenko.spring.forge.example.WithPostgres;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class AccountControllerTest extends WithPostgres {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM account");

        jdbcTemplate.update("INSERT INTO account (account_number, acc_type, interest_rate) VALUES (?, ?, ?)", "S001", "S", 0.05);
        jdbcTemplate.update("INSERT INTO account (account_number, acc_type, overdraft_limit) VALUES (?, ?, ?)", "C001", "C", 1000.0);
    }

    @Test
    void testFilterByAccountType() throws Exception {
        mockMvc.perform(get("/accounts")
                .param("accType", "S"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].accountNumber", is("S001")));

        mockMvc.perform(get("/accounts")
                .param("accType", "C"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].accountNumber", is("C001")));
    }

    @Test
    void testFilterByAccountTypeMultiple() throws Exception {
        mockMvc.perform(get("/accounts")
                .param("accType", "S", "C"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)));
    }
}
