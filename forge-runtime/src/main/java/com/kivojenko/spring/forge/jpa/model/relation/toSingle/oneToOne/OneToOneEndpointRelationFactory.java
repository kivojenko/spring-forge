package com.kivojenko.spring.forge.jpa.model.relation.toSingle.oneToOne;

import com.kivojenko.spring.forge.jpa.model.relation.EndpointRelation;
import com.kivojenko.spring.forge.jpa.model.relation.EndpointRelationFactory;
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
}
