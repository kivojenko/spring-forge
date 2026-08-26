package com.kivojenko.spring.forge.example.model.slashes;

import com.kivojenko.spring.forge.annotation.WithJpaRepository;
import com.kivojenko.spring.forge.annotation.WithRestController;
import com.kivojenko.spring.forge.annotation.WithService;
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
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "documents")
@WithJpaRepository
@WithService
@WithRestController(allowSlashes = true)
public class Document {
  @Id
  private String id;
  private String title;
  private String content;
}
