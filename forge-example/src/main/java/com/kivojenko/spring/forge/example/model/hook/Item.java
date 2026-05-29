package com.kivojenko.spring.forge.example.model.hook;

import com.kivojenko.spring.forge.annotation.WithRestController;
import com.kivojenko.spring.forge.annotation.endpoint.WithEndpoints;
import com.kivojenko.spring.forge.jpa.contract.persistence.ForgePersistenceAspect;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
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
 * An item entity to showcase {@link ForgePersistenceAspect} usage.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "items")
@WithRestController
public class Item {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Double price;

    @OneToMany(mappedBy = "item", cascade = CascadeType.ALL, orphanRemoval = true)
    @WithEndpoints
    @Builder.Default
    private List<SubItem> subItems = new ArrayList<>();

    @ManyToMany
    @JoinTable(
        name = "item_categories",
        joinColumns = @JoinColumn(name = "item_id"),
        inverseJoinColumns = @JoinColumn(name = "category_id")
    )
    @WithEndpoints
    @Builder.Default
    private List<ItemCategory> categories = new ArrayList<>();
}
