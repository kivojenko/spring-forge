package com.kivojenko.spring.forge.example.model.filter;

import com.kivojenko.spring.forge.annotation.WithRestController;
import com.kivojenko.spring.forge.annotation.filter.FilterField;
import com.kivojenko.spring.forge.annotation.filter.FilterSearchField;
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
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "posts")
@WithRestController
public class Post {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @FilterSearchField(stringMatchMode = StringMatchMode.CONTAINS_IGNORE_CASE)
  private String title;

  @FilterSearchField(stringMatchMode = StringMatchMode.CONTAINS_IGNORE_CASE)
  private String content;

  @FilterSearchField(stringMatchMode = StringMatchMode.STARTS_WITH)
  private String slug;

  @ElementCollection
  @CollectionTable(name = "post_tags", joinColumns = @JoinColumn(name = "post_id"))
  @Column(name = "tag")
  @Builder.Default
  @FilterSearchField(stringMatchMode = StringMatchMode.CONTAINS_IGNORE_CASE)
  private List<String> tags = new ArrayList<>();

  // Standalone filter field alongside search fields
  @FilterField
  private Boolean published;

  // Custom named search field: ?query=...
  @FilterSearchField(name = "query", stringMatchMode = StringMatchMode.CONTAINS_IGNORE_CASE)
  private String authorName;
}
