package com.kivojenko.spring.forge.example.model.filter;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** A labelled link of a {@link Post}, searched through a collection of embeddables. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class PostLink {
  private String label;
  private String url;
}
