package com.kivojenko.spring.forge.example.model.filter;

import com.kivojenko.spring.forge.annotation.WithRestController;
import com.kivojenko.spring.forge.annotation.filter.FilterField;
import com.kivojenko.spring.forge.annotation.filter.IterableFilterField;
import com.kivojenko.spring.forge.annotation.filter.IterableMatchMode;
import com.kivojenko.spring.forge.annotation.filter.NumberRangeFilterField;
import com.kivojenko.spring.forge.annotation.filter.StringFilterField;
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

  @StringFilterField(match = StringMatchMode.CONTAINS)
  private String name;

  @StringFilterField(match = StringMatchMode.EQUALS_IGNORE_CASE)
  private String sku;

  @NumberRangeFilterField
  private BigDecimal price;

  @FilterField
  private Boolean active;

  @StringFilterField
  private String brand;

  @FilterField
  private boolean inStock;

  @FilterField
  private Double weight;

  @ManyToOne
  @FilterField
  private ProductCategory category;

  @ManyToMany
  @IterableFilterField(match = IterableMatchMode.ANY)
  @Builder.Default
  private Set<Tag> tags = new HashSet<>();
}
