package com.kivojenko.spring.forge.example.repository;

import com.kivojenko.spring.forge.example.WithPostgres;
import com.kivojenko.spring.forge.example.model.InterfaceEntityForgeRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class InterfaceEntityRepositoryTest extends WithPostgres {

    @Autowired
    private InterfaceEntityForgeRepository repository;

    @Test
    void testCustomInterfaceMethod() {
        assertEquals("custom", repository.customMethod());
    }
}
