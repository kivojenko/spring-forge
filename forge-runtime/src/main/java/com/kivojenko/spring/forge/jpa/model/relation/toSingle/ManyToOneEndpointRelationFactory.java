package com.kivojenko.spring.forge.jpa.model.relation.toSingle;

import com.kivojenko.spring.forge.jpa.model.relation.EndpointRelation;
import com.kivojenko.spring.forge.jpa.model.relation.EndpointRelationFactory;
import com.kivojenko.spring.forge.jpa.model.relation.toSingle.manyToOne.LinkExistingManyToOneEndpointRelation;
import com.kivojenko.spring.forge.jpa.model.relation.toSingle.manyToOne.ReadManyToOneEndpointRelation;
import com.kivojenko.spring.forge.jpa.model.relation.toSingle.manyToOne.UnlinkManyToOneEndpointRelation;
import lombok.experimental.SuperBuilder;

@SuperBuilder
public class ManyToOneEndpointRelationFactory extends EndpointRelationFactory {
  @Override
  public EndpointRelation getReadRelation() {
    return ReadManyToOneEndpointRelation
        .builder()
        .path(path)
        .field(field)
        .entityModel(entityModel)
        .targetEntityModel(targetEntityModel)
        .build();
  }

  @Override
  public EndpointRelation getLinkExistingRelation() {
    return LinkExistingManyToOneEndpointRelation
        .builder()
        .path(path)
        .field(field)
        .entityModel(entityModel)
        .targetEntityModel(targetEntityModel)
        .build();
  }

  @Override
  public EndpointRelation getUnlinkRelation() {
    return UnlinkManyToOneEndpointRelation
        .builder()
        .path(path)
        .field(field)
        .entityModel(entityModel)
        .targetEntityModel(targetEntityModel)
        .build();
  }
}
