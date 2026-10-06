package com.kivojenko.spring.forge.example.model.fetch;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;
import com.kivojenko.spring.forge.annotation.WithRestController;
import com.kivojenko.spring.forge.annotation.endpoint.WithEndpoints;
import jakarta.persistence.*;
import java.util.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "fetch_parents")
@WithRestController
public class FetchParent {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @JsonView(FetchViews.Summary.class)
  private Long id;

  @JsonView(FetchViews.Summary.class)
  private String name;

  @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL)
  @Builder.Default
  @WithEndpoints(view = FetchViews.Summary.class)
  @JsonIgnore
  private List<FetchChild> children = new ArrayList<>();
}
