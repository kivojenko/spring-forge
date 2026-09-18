package com.kivojenko.spring.forge.example.model.filter;

import com.kivojenko.spring.forge.annotation.WithRestController;
import com.kivojenko.spring.forge.annotation.filter.FilterField;
import com.kivojenko.spring.forge.annotation.filter.StringMatchMode;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
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
@Table(name = "tracks")
@WithRestController
public class Track {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  // A *_ANY mode takes a Set of values: ?name=Alfa&name=Beta matches either of them, ignoring case
  @FilterField(stringMatchMode = StringMatchMode.EQUALS_ANY_IGNORE_CASE)
  private String name;

  // A second mapping under the same parameter name: ?name= is matched against both columns
  @FilterField(name = "name", stringMatchMode = StringMatchMode.EQUALS_ANY_IGNORE_CASE)
  private String originalName;

  // Several loose matches at once, and rows without a label are kept
  @FilterField(stringMatchMode = StringMatchMode.CONTAINS_ANY_IGNORE_CASE, orNull = true)
  private String label;

  // An @ElementCollection is matched element-wise: any of the values against any of the elements
  @ElementCollection
  @CollectionTable(name = "track_tags", joinColumns = @JoinColumn(name = "track_id"))
  @Column(name = "tag")
  @Builder.Default
  @FilterField(stringMatchMode = StringMatchMode.STARTS_WITH_ANY)
  private List<String> tags = new ArrayList<>();

  // Multi-value members of a family: either credit may match any of the values sent for it
  @FilterField(family = "credits", stringMatchMode = StringMatchMode.CONTAINS_ANY_IGNORE_CASE)
  private String artist;

  @FilterField(family = "credits", stringMatchMode = StringMatchMode.CONTAINS_ANY_IGNORE_CASE)
  private String composer;
}
