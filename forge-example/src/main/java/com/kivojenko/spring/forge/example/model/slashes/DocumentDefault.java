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
@Table(name = "documents_default")
@WithJpaRepository
@WithService
@WithRestController
public class DocumentDefault {
  @Id
  private String id;
  private String title;
  private String content;
}
