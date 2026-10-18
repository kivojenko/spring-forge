package com.kivojenko.spring.forge.example.model.collection;

import com.kivojenko.spring.forge.annotation.GetOrCreate;
import com.kivojenko.spring.forge.annotation.WithRestController;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Get-or-create by collection elements: any CAS number, any synonym, or a name together with any hazard code.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "chemicals")
@WithRestController
@GetOrCreate(fields = "numberReferences.casNumbers.number")  // POST /chemicals/get-or-create?numberReferences.casNumbers.number=7732-18-5
@GetOrCreate(fields = "synonyms")                            // POST /chemicals/get-or-create?synonyms=Aqua
@GetOrCreate(fields = {"name", "hazards.code"})              // POST /chemicals/get-or-create?name=Ethanol&hazards.code=H225
public class Chemical {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String name;

  @Embedded
  private NumberReferences numberReferences;

  @ElementCollection
  @CollectionTable(name = "chemical_synonyms")
  @Builder.Default
  private Set<String> synonyms = new HashSet<>();

  @ManyToMany
  @JoinTable(name = "chemical_hazards")
  private List<Hazard> hazards;
}
