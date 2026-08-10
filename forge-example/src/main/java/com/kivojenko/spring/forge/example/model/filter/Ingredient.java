package com.kivojenko.spring.forge.example.model.filter;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.kivojenko.spring.forge.annotation.WithRestController;
import com.kivojenko.spring.forge.annotation.endpoint.WithEndpoints;
import com.kivojenko.spring.forge.annotation.filter.FilterField;
import com.kivojenko.spring.forge.annotation.filter.StringMatchMode;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.HashSet;
import java.util.Set;
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
@Table(name = "ingredients")
@WithRestController
public class Ingredient {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  // First declared: becomes the only active filter when two fields share the same exposed name
  @FilterField(name = "name", stringMatchMode = StringMatchMode.CONTAINS_IGNORE_CASE)
  private String name;

  @OneToMany(mappedBy = "ingredient", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
  @Builder.Default
  @WithEndpoints // to allow POST /ingredients/{id}/alternativeNames
  @FilterField(name = "name", targetField = "name")
  @JsonManagedReference
  private Set<IngredientAlternativeName> alternativeNames = new HashSet<>();
}
