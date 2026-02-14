package com.kivojenko.spring.forge.example.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.kivojenko.spring.forge.annotation.GetOrCreate;
import com.kivojenko.spring.forge.annotation.WithRestController;
import com.kivojenko.spring.forge.annotation.endpoint.WithEndpoints;
import com.kivojenko.spring.forge.jpa.contract.HasName;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ToString
@RequiredArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "categories")
@WithRestController
@GetOrCreate
public class Category implements HasName {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "name_translation_id")
    @WithEndpoints
    private Translation nameTranslation;

    @ManyToMany(mappedBy = "categories")
    @Builder.Default
    @ToString.Exclude
    @JsonIgnore
    private List<Book> books = new ArrayList<>();
}
