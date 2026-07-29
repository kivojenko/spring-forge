package com.kivojenko.spring.forge.example.model.inheritance;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.Setter;

@Entity
@DiscriminatorValue("2")
@Getter
@Setter
public class Developer extends Employee {
    private String programmingLanguage;
}
