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
@Table(name = "articles")
@WithRestController
public class Article {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  // The "search" family: title, summary and keywords are OR-ed with each other …
  @FilterField(family = "search", stringMatchMode = StringMatchMode.CONTAINS_IGNORE_CASE)
  private String title;

  @FilterField(family = "search", stringMatchMode = StringMatchMode.CONTAINS_IGNORE_CASE)
  private String summary;

  @ElementCollection
  @CollectionTable(name = "article_keywords", joinColumns = @JoinColumn(name = "article_id"))
  @Column(name = "keyword")
  @Builder.Default
  @FilterField(family = "search", stringMatchMode = StringMatchMode.CONTAINS_IGNORE_CASE)
  private List<String> keywords = new ArrayList<>();

  // … and the family as a whole is AND-ed with this one
  @FilterField
  private Boolean published;
}
