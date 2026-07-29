package com.kivojenko.spring.forge.example.model.inheritance;

import com.kivojenko.spring.forge.annotation.WithJpaRepository;
import com.kivojenko.spring.forge.annotation.WithRestController;
import com.kivojenko.spring.forge.annotation.WithService;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "acc_type", discriminatorType = DiscriminatorType.CHAR)
@DiscriminatorValue("A")
@WithJpaRepository
@WithService
@WithRestController
@Getter
@Setter
public abstract class Account {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String accountNumber;
}
