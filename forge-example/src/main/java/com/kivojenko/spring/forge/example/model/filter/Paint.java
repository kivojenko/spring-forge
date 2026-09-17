package com.kivojenko.spring.forge.example.model.filter;

import com.kivojenko.spring.forge.annotation.WithRestController;
import com.kivojenko.spring.forge.annotation.filter.FilterField;
import com.kivojenko.spring.forge.annotation.filter.StringMatchMode;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
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
@Table(name = "paints")
@WithRestController
public class Paint {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @FilterField(stringMatchMode = StringMatchMode.CONTAINS)
  private String name;

  // targetField pointing at a collection inside the embeddable: dye.hexColors.any()
  @Embedded
  @FilterField(name = "hexColor", targetField = "hexColors", stringMatchMode = StringMatchMode.EQUALS_IGNORE_CASE)
  @FilterField(name = "hasHexColors", targetField = "hexColors", isPresent = true)
  @FilterField(name = "colorIndex", targetField = "colorIndex")
  // Same target with the default parameter name, and matching rows without any colour at all
  @FilterField(targetField = "hexColors", stringMatchMode = StringMatchMode.STARTS_WITH, orNull = true)
  private Dye dye;

  // @ElementCollection of scalars filtered directly: keywords.any()
  @ElementCollection
  @CollectionTable(name = "paint_keywords", joinColumns = @JoinColumn(name = "paint_id"))
  @Column(name = "keyword")
  @Builder.Default
  @FilterField(stringMatchMode = StringMatchMode.CONTAINS_IGNORE_CASE)
  private List<String> keywords = new ArrayList<>();
}
