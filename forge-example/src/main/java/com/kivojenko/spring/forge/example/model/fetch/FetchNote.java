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
@Table(name = "fetch_notes")
public class FetchNote {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @JsonView(FetchViews.Summary.class)
  private Long id;

  @JsonView(FetchViews.Summary.class)
  private String name;
}
