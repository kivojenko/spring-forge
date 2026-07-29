package com.kivojenko.spring.forge.example.model.inheritance;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.Setter;

@Entity
@DiscriminatorValue("S")
@Getter
@Setter
public class SavingsAccount extends Account {
    private double interestRate;
}
