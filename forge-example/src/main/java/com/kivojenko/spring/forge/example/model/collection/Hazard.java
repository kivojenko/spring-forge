package com.kivojenko.spring.forge.example.model.collection;

import com.kivojenko.spring.forge.annotation.WithRestController;
import jakarta.persistence.Entity;
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
@Table(name = "hazards")
@WithRestController
public class Hazard {

  @Id
  private String code;

  private String description;
}
