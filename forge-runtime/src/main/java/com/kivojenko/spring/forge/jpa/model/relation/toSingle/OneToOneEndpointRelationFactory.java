package com.kivojenko.spring.forge.jpa.model.relation.toSingle;

import com.kivojenko.spring.forge.jpa.model.relation.EndpointRelation;
import com.kivojenko.spring.forge.jpa.model.relation.EndpointRelationFactory;
import com.kivojenko.spring.forge.jpa.model.relation.toSingle.oneToOne.ReadOneToOneEndpointRelation;
import com.kivojenko.spring.forge.jpa.model.relation.toSingle.oneToOne.UnlinkOneToOneEndpointRelation;
import com.kivojenko.spring.forge.jpa.model.relation.toSingle.oneToOne.AddNewEmbeddedOneToOneEndpointRelation;
import com.kivojenko.spring.forge.jpa.model.relation.toSingle.oneToOne.AddNewEntityOneToOneEndpointRelation;
import jakarta.persistence.Embedded;
import lombok.experimental.SuperBuilder;

@SuperBuilder
public class OneToOneEndpointRelationFactory extends EndpointRelationFactory {
  @Override
  public EndpointRelation getReadRelation() {
    return ReadOneToOneEndpointRelation
        .builder()
        .path(path)
        .field(field)
        .entityModel(entityModel)
        .targetEntityModel(targetEntityModel)
        .build();
  }

  @Override
  public EndpointRelation getUnlinkRelation() {
    return UnlinkOneToOneEndpointRelation
        .builder()
        .path(path)
        .field(field)
        .entityModel(entityModel)
        .targetEntityModel(targetEntityModel)
        .build();
  }

  @Override
  public EndpointRelation getAddNewRelation() {
    // For @Embedded generate a setter-style POST; for entity-valued OneToOne generate create-and-link POST
    if (field.getAnnotation(Embedded.class) != null) {
      return AddNewEmbeddedOneToOneEndpointRelation
          .builder()
          .path(path)
          .field(field)
          .entityModel(entityModel)
          .targetEntityModel(targetEntityModel)
          .build();
    }

    return AddNewEntityOneToOneEndpointRelation
        .builder()
        .path(path)
        .field(field)
        .entityModel(entityModel)
        .targetEntityModel(targetEntityModel)
        .build();
  }
}
