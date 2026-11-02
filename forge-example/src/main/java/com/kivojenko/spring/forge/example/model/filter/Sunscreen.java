package com.kivojenko.spring.forge.example.model.filter;

import com.kivojenko.spring.forge.annotation.WithRestController;
import com.kivojenko.spring.forge.annotation.filter.ComparisonMatchMode;
import com.kivojenko.spring.forge.annotation.filter.FilterField;
import com.kivojenko.spring.forge.annotation.filter.StringMatchMode;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@RequiredArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "sunscreens")
@WithRestController
public class Sunscreen {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  // Outside the family — AND-ed with it, like any other filter
  @FilterField(stringMatchMode = StringMatchMode.CONTAINS_IGNORE_CASE)
  private String name;

  // The "protection" family: each member's own min and max are AND-ed into one range, and those ranges
  // are OR-ed with each other. Exposes spf, minSpf and maxSpf
  @FilterField(family = "protection")
  private Integer spf;

  // Exposes minMinSpf and maxMinSpf
  @FilterField(family = "protection", comparisonMatchMode = ComparisonMatchMode.RANGE)
  private Integer minSpf;

  // Exposes minMaxSpf and maxMaxSpf
  @FilterField(family = "protection", comparisonMatchMode = ComparisonMatchMode.RANGE)
  private Integer maxSpf;
}
