package com.kivojenko.spring.forge.example;

import com.kivojenko.spring.forge.annotation.GetOrCreate;
import com.kivojenko.spring.forge.annotation.WithRestController;
import com.kivojenko.spring.forge.annotation.WithService;
import com.kivojenko.spring.forge.annotation.endpoint.WithEndpoints;
import com.kivojenko.spring.forge.jpa.contract.HasName;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * A minimal example entity to demonstrate how to use Lombok and Spring Forge annotations together.
 * Highlights:
 * - Lombok keeps the entity concise.
 * - JPA annotations define persistence mapping.
 * - Forge annotations instruct the annotation processor to generate Repository, Service and REST Controller.
 * - Implements {@link HasName} so "get or create" helper can be generated.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "authors")
@WithRestController
@GetOrCreate
public class Author implements HasName {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /**
   * A human‑readable name, used by HasName and GetOrCreate.
   */
  @Column(nullable = false, unique = true)
  private String name;

  /**
   * Books written by this author. Endpoints will be generated for this association.
   */
  @OneToMany(mappedBy = "author", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  @WithEndpoints
  private List<Book> books = new ArrayList<>();
}
