package com.kivojenko.spring.forge.example.model.filter;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.kivojenko.spring.forge.annotation.WithJpaRepository;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "ingredient_alternative_names")
@WithJpaRepository
public class IngredientAlternativeName {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String name;

  @ManyToOne
  @JoinColumn(name = "ingredient_id")
  @JsonBackReference
  private Ingredient ingredient;
}
