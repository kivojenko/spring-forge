package com.kivojenko.spring.forge.example.model.inheritance;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.Setter;

@Entity
@DiscriminatorValue("C")
@Getter
@Setter
public class CheckingAccount extends Account {
    private double overdraftLimit;
}
