package com.kivojenko.spring.forge.example.model.fetch;

import com.fasterxml.jackson.annotation.JsonView;
import jakarta.persistence.*;
import java.util.*;
import lombok.*;

/** Embedded, with an EAGER collection that nobody asks for by name. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Embeddable
public class FetchMeta {
  @JsonView(FetchViews.Summary.class)
  private String source;

  @ManyToMany(fetch = FetchType.EAGER)
  @JoinTable(name = "fetch_child_labels")
  @Builder.Default
  @JsonView(FetchViews.Summary.class)
  private Set<FetchTag> labels = new HashSet<>();
}
