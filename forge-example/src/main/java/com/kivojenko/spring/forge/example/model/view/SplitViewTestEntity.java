package com.kivojenko.spring.forge.example.model.view;

import com.fasterxml.jackson.annotation.JsonView;
import com.kivojenko.spring.forge.annotation.WithRestController;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
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
@Table(name = "split_view_test_entities")
@WithRestController(listView = Views.Summary.class, detailView = Views.Detail.class)
public class SplitViewTestEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @JsonView(Views.Summary.class)
  private Long id;

  @JsonView(Views.Summary.class)
  private String name;

  @JsonView(Views.Detail.class)
  private String secretNote;
}
