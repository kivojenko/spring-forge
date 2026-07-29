package com.kivojenko.spring.forge.example.model.inheritance;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.Setter;

@Entity
@DiscriminatorValue("1")
@Getter
@Setter
public class Manager extends Employee {
    private int teamSize;
}
