package com.kivojenko.spring.forge.example.model.filter;

import com.kivojenko.spring.forge.annotation.WithRestController;
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
@Table(name = "lotions")
@WithRestController
public class Lotion {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  // Outside the shared parameter — AND-ed with it, like any other filter
  @FilterField(stringMatchMode = StringMatchMode.CONTAINS_IGNORE_CASE)
  private String name;

  // uvFilter reads both flags; chemicalUvFilter and mineralUvFilter each read their own
  @FilterField(name = "uvFilter")
  @FilterField
  private boolean chemicalUvFilter;

  @FilterField(name = "uvFilter")
  @FilterField
  private boolean mineralUvFilter;
}
