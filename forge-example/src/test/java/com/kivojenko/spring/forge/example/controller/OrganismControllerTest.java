package com.kivojenko.spring.forge.example.controller;

import com.kivojenko.spring.forge.example.WithPostgres;
import com.kivojenko.spring.forge.example.model.generics.Organism;
import com.kivojenko.spring.forge.example.model.generics.OrganismForgeRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Transactional
public class OrganismControllerTest extends WithPostgres {

    @Autowired
    private OrganismForgeRepository organismRepository;

    @Test
    void shouldFindByNameUsingGenericInterfaceMethod() throws Exception {
        Organism organism = new Organism();
        organism.setNameEnUS("Amoeba");
        organismRepository.save(organism);

        mockMvc.perform(post("/organisms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nameEnUS\": \"Paramecium\"}"))
                .andExpect(status().isCreated());

        assert organismRepository.findByNameEnUS("Amoeba").isPresent();
    }

    @Test
    void shouldAccessOrganismViaEndpoint() throws Exception {
        Organism organism = new Organism();
        organism.setNameEnUS("Amoeba");
        organismRepository.save(organism);

        mockMvc.perform(get("/organisms"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].nameEnUS").value("Amoeba"));
    }
}
