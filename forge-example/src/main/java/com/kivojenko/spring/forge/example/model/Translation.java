package com.kivojenko.spring.forge.example.model;

import com.kivojenko.spring.forge.annotation.WithRestController;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@WithRestController(makeAbstract = true)
@Getter
@Setter
public class Translation {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(length = 100000)
  private String enUS;

  @Nullable
  @Column(length = 100000)
  private String ruRU;

  public String getTranslation(String locale) {
    return locale.equals("enUS") ? enUS : ruRU;
  }
}
