package com.kivojenko.spring.forge.example.model.inheritance;

import com.kivojenko.spring.forge.annotation.WithJpaRepository;
import com.kivojenko.spring.forge.annotation.WithRestController;
import com.kivojenko.spring.forge.annotation.WithService;
import com.kivojenko.spring.forge.annotation.endpoint.WithEndpoints;
import com.kivojenko.spring.forge.annotation.filter.FilterField;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "vehicle_type")
@WithJpaRepository
@WithService
@WithRestController
@Getter
@Setter
public abstract class Vehicle {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @FilterField
    private String name;

    @OneToMany(mappedBy = "vehicle", cascade = CascadeType.ALL, orphanRemoval = true)
    @WithEndpoints
    @JsonIgnore
    private List<MaintenanceRecord> maintenanceRecords = new ArrayList<>();
}
