package com.kivojenko.spring.forge.example.model.filter;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.JoinColumn;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Embeddable value object holding a collection — the target of {@code @FilterField(targetField = "hexColors")}.
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Dye {

  private Long colorIndex;

  @ElementCollection
  @CollectionTable(name = "paint_hex_colors", joinColumns = @JoinColumn(name = "paint_id"))
  @Column(name = "hex_color")
  @Builder.Default
  private List<String> hexColors = new ArrayList<>();
}
