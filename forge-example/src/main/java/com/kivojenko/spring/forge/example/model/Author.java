package com.kivojenko.spring.forge.example.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.kivojenko.spring.forge.annotation.GetOrCreate;
import com.kivojenko.spring.forge.annotation.WithRestController;
import com.kivojenko.spring.forge.annotation.endpoint.WithEndpoints;
import com.kivojenko.spring.forge.annotation.endpoint.WithGetEndpoint;
import com.kivojenko.spring.forge.jpa.contract.HasName;
import jakarta.persistence.*;
import lombok.*;

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
@Getter
@Setter
@ToString
@RequiredArgsConstructor
@Builder
@Entity
@Table(name = "authors")
@WithRestController
@GetOrCreate
@AllArgsConstructor
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
  @ToString.Exclude
  @JsonIgnore
  private List<Book> books = new ArrayList<>();

  @WithGetEndpoint
  @JsonIgnore
  public List<String> getBooksTitles() {
    return books.stream().map(Book::getTitle).toList();
  }
}
