package com.kivojenko.spring.forge.example.model;

import com.kivojenko.spring.forge.annotation.WithJpaRepository;
import com.kivojenko.spring.forge.annotation.WithRestController;
import com.kivojenko.spring.forge.annotation.WithService;
import com.kivojenko.spring.forge.example.repository.CustomRepository;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@WithJpaRepository(interfaces = {CustomRepository.class})
@WithService
@WithRestController
public class InterfaceEntity {
    @Id
    @GeneratedValue
    private Long id;
    private String name;
}
