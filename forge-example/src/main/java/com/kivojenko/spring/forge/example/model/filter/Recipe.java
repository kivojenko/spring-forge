package com.kivojenko.spring.forge.example.model.filter;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.kivojenko.spring.forge.annotation.WithRestController;
import com.kivojenko.spring.forge.annotation.filter.FilterField;
import com.kivojenko.spring.forge.annotation.filter.IterableMatchMode;
import com.kivojenko.spring.forge.annotation.filter.StringMatchMode;
import jakarta.persistence.Entity;
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
@Table(name = "recipes")
@WithRestController
public class Recipe {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String name;

  // ALL through a targetField: ?spices=1,2 takes a Set<Long> and every id must be found on some element
  @OneToMany(mappedBy = "recipe")
  @JsonIgnore
  @Builder.Default
  @FilterField(name = "spices", targetField = "spice.id", iterableMatchMode = IterableMatchMode.ALL)
  @FilterField(name = "spiceNames", targetField = "spice.name", iterableMatchMode = IterableMatchMode.ALL)
  // ANY of several values: a *_ANY string match mode OR-s the values instead
  @FilterField(name = "anySpiceNames", targetField = "spice.name", stringMatchMode = StringMatchMode.EQUALS_ANY)
  // The default stays a single value matched against any element
  @FilterField(name = "spiceName", targetField = "spice.name")
  private Set<RecipeSpice> recipeSpices = new HashSet<>();
}
