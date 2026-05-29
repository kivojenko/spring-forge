package com.kivojenko.spring.forge.example.model.hook;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.kivojenko.spring.forge.annotation.WithJpaRepository;
import com.kivojenko.spring.forge.annotation.WithRestController;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * A subitem entity associated with {@link Item}.
 */
@Getter
@Setter
@ToString(exclude = "item")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "sub_items")
@WithJpaRepository
public class SubItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @ManyToOne
    @JsonIgnore
    private Item item;
}
