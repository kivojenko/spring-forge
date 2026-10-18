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
@Table(name = "cas_numbers")
@WithRestController
public class Cas {

  @Id
  private String number;
}
