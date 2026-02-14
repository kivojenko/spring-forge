package com.kivojenko.spring.forge.example.model;

import com.kivojenko.spring.forge.annotation.WithRestController;
import com.kivojenko.spring.forge.annotation.endpoint.WithEndpoints;
import jakarta.persistence.*;
import lombok.*;

/**
 * A simple entity that belongs to an {@link Author}.
 */
@Getter
@Setter
@ToString
@RequiredArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "books")
@WithRestController
public class Book {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "book_categories",
        joinColumns = @JoinColumn(name = "book_id"),
        inverseJoinColumns = @JoinColumn(name = "category_id")
    )
    @WithEndpoints
    @Builder.Default
    @ToString.Exclude
    private java.util.List<Category> categories = new java.util.ArrayList<>();

    @ManyToOne(optional = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    @WithEndpoints
    @ToString.Exclude
    private Author author;
}
