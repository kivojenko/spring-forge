package com.kivojenko.spring.forge.example.model.hook;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.kivojenko.spring.forge.annotation.WithJpaRepository;
import com.kivojenko.spring.forge.annotation.WithRestController;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

/**
 * A category entity to showcase Many-to-Many association with {@link Item}.
 */
@Getter
@Setter
@ToString(exclude = "items")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "categories")
@WithJpaRepository
public class ItemCategory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @ManyToMany(mappedBy = "categories")
    @Builder.Default
    @JsonIgnore
    private List<Item> items = new ArrayList<>();
}
