package com.kivojenko.spring.forge.example;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ForgeExampleApplicationTests extends WithPostgres {
    @Test
    void contextLoads() {
    }
}
