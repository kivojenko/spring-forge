package com.kivojenko.spring.forge.example;

import com.kivojenko.spring.forge.annotation.WithRestController;
import com.kivojenko.spring.forge.annotation.WithService;
import com.kivojenko.spring.forge.annotation.endpoint.WithEndpoints;
import jakarta.persistence.*;
import lombok.*;

/**
 * A simple entity that belongs to an {@link Author}.
 */
@Data
@NoArgsConstructor
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

    @Column(nullable = false)
    private String category;

    @ManyToOne(optional = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    @WithEndpoints
    private Author author;
}
