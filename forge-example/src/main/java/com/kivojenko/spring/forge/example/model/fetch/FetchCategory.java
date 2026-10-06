package com.kivojenko.spring.forge.example.model.fetch;

import com.fasterxml.jackson.annotation.JsonView;
import jakarta.persistence.*;
import java.util.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "fetch_categories")
public class FetchCategory {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @JsonView(FetchViews.Summary.class)
  private Long id;

  @JsonView(FetchViews.Summary.class)
  private String name;

  /** Self-referencing: the chain is followed until it ends. */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "parent_id")
  @JsonView(FetchViews.Summary.class)
  private FetchCategory parent;
}
