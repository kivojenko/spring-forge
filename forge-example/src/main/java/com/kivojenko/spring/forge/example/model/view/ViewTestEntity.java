package com.kivojenko.spring.forge.example.model.view;

import com.fasterxml.jackson.annotation.JsonView;
import com.kivojenko.spring.forge.annotation.WithRestController;
import com.kivojenko.spring.forge.annotation.endpoint.WithEndpoints;
import com.kivojenko.spring.forge.annotation.endpoint.WithGetEndpoint;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "view_test_entities")
@WithRestController(view = Views.Summary.class)
public class ViewTestEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @JsonView(Views.Summary.class)
  private Long id;

  @JsonView(Views.Summary.class)
  private String name;

  @JsonView(Views.Detail.class)
  private String secretNote;

  @OneToMany(mappedBy = "viewTestEntity", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  @Builder.Default
  @WithEndpoints(view = Views.Summary.class)
  private List<ViewTestChild> children = new ArrayList<>();

  @WithGetEndpoint(view = Views.Summary.class)
  public List<ViewTestChild> getSpecialChildren() {
    return children;
  }
}
