package com.kivojenko.spring.forge.example.model.filter;

import com.kivojenko.spring.forge.annotation.WithRestController;
import com.kivojenko.spring.forge.annotation.filter.FamilyMatchMode;
import com.kivojenko.spring.forge.annotation.filter.FilterField;
import com.kivojenko.spring.forge.annotation.filter.FilterFamily;
import com.kivojenko.spring.forge.annotation.filter.StringMatchMode;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@RequiredArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "contacts")
@WithRestController
// Only contacts that can actually be reached are listed: at least one member of the family must hold a
// value, whether or not any of its parameters is sent
@FilterFamily(name = "reachability", matchMode = FamilyMatchMode.EXISTS)
public class Contact {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  // Outside the family — AND-ed with it, like any other filter
  @FilterField(stringMatchMode = StringMatchMode.CONTAINS_IGNORE_CASE)
  private String name;

  @FilterField(family = "reachability", stringMatchMode = StringMatchMode.CONTAINS_IGNORE_CASE)
  private String email;

  @FilterField(family = "reachability", stringMatchMode = StringMatchMode.CONTAINS_IGNORE_CASE)
  private String phone;

  // A collection member contributes isNotEmpty() rather than isNotNull()
  @ElementCollection
  @CollectionTable(name = "contact_handles", joinColumns = @JoinColumn(name = "contact_id"))
  @Column(name = "handle")
  @Builder.Default
  @FilterField(family = "reachability", stringMatchMode = StringMatchMode.CONTAINS_IGNORE_CASE)
  private List<String> handles = new ArrayList<>();
}
