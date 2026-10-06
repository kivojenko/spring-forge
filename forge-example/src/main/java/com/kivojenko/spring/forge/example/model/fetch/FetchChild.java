package com.kivojenko.spring.forge.example.model.fetch;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;
import com.kivojenko.spring.forge.annotation.WithRestController;
import jakarta.persistence.*;
import java.util.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "fetch_children")
@WithRestController
public class FetchChild {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @JsonView(FetchViews.Summary.class)
  private Long id;

  @JsonView(FetchViews.Summary.class)
  private String title;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "parent_id")
  @JsonIgnore
  private FetchParent parent;

  /** EAGER to-one: one select per row if nothing fetches it. */
  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "category_id")
  @JsonView(FetchViews.Summary.class)
  private FetchCategory category;

  /** LAZY to-one the view writes. */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "author_id")
  @JsonView(FetchViews.Summary.class)
  private FetchOwner author;

  /** LAZY collection the view writes. */
  @ManyToMany
  @JoinTable(name = "fetch_child_tags")
  @Builder.Default
  @JsonView(FetchViews.Summary.class)
  private Set<FetchTag> tags = new HashSet<>();

  /** Embedded, with an EAGER collection inside. */
  @Embedded
  @Builder.Default
  @JsonView(FetchViews.Summary.class)
  private FetchMeta meta = new FetchMeta();

  /** Hidden from the view by a meta-annotation: must not be loaded. */
  @OneToMany
  @JoinColumn(name = "note_child_id")
  @Builder.Default
  @DetailOnly
  private Set<FetchNote> notes = new HashSet<>();

  /** Hidden by @JsonIgnore: must not be loaded. */
  @ManyToMany
  @JoinTable(name = "fetch_child_secrets")
  @Builder.Default
  @JsonIgnore
  private Set<FetchSecret> secrets = new HashSet<>();
}
