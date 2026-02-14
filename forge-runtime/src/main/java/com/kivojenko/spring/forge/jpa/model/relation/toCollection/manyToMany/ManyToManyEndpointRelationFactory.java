package com.kivojenko.spring.forge.jpa.model.relation.toCollection.manyToMany;

import com.kivojenko.spring.forge.jpa.model.relation.EndpointRelation;
import com.kivojenko.spring.forge.jpa.model.relation.EndpointRelationFactory;
import lombok.experimental.SuperBuilder;

@SuperBuilder
public class ManyToManyEndpointRelationFactory extends EndpointRelationFactory {

  @Override
  public EndpointRelation getReadRelation() {
    return ReadManyToManyEndpointRelation
        .builder()
        .path(path)
        .field(field)
        .entityModel(entityModel)
        .targetEntityModel(targetEntityModel)
        .build();
  }

  @Override
  public EndpointRelation getLinkExistingRelation() {
    return LinkExistingManyToManyEndpointRelation
        .builder()
        .path(path)
        .field(field)
        .entityModel(entityModel)
        .targetEntityModel(targetEntityModel)
        .build();
  }

  @Override
  public EndpointRelation getUnlinkRelation() {
    return UnlinkManyToManyEndpointRelation
        .builder()
        .path(path)
        .field(field)
        .entityModel(entityModel)
        .targetEntityModel(targetEntityModel)
        .build();
  }
}
