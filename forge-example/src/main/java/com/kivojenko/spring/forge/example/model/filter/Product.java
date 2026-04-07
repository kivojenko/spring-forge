package com.kivojenko.spring.forge.example.model.filter;

import com.kivojenko.spring.forge.annotation.WithRestController;
import com.kivojenko.spring.forge.annotation.filter.ComparisonMatchMode;
import com.kivojenko.spring.forge.annotation.filter.FilterField;
import com.kivojenko.spring.forge.annotation.filter.IterableMatchMode;
import com.kivojenko.spring.forge.annotation.filter.StringMatchMode;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@RequiredArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "products")
@WithRestController
public class Product {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @FilterField(stringMatchMode = StringMatchMode.CONTAINS)
  private String name;

  @FilterField(stringMatchMode = StringMatchMode.EQUALS_IGNORE_CASE)
  private String sku;

  @FilterField
  private BigDecimal price;

  @FilterField
  private LocalDateTime createdAt;

  @FilterField
  private Boolean active;

  @FilterField
  private String brand;

  @FilterField
  private boolean inStock;

  @FilterField(comparisonMatchMode = ComparisonMatchMode.EXACT)
  private Double weight;

  @ManyToOne
  @FilterField
  private ProductCategory category;

  @ManyToMany
  @FilterField(iterableMatchMode = IterableMatchMode.ANY)
  @Builder.Default
  private Set<Tag> tags = new HashSet<>();
}
