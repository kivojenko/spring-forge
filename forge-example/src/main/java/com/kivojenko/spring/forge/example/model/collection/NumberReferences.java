package com.kivojenko.spring.forge.example.model.collection;

import jakarta.persistence.Embeddable;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import java.util.Set;
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
@Embeddable
public class NumberReferences {

  // No initializer: get-or-create initializes the collection (and the embedded object) when null
  @ManyToMany
  @JoinTable(name = "chemical_cas_numbers")
  private Set<Cas> casNumbers;
}
