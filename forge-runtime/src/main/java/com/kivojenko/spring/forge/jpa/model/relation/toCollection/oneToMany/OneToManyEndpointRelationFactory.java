package com.kivojenko.spring.forge.jpa.model.relation.toCollection.oneToMany;

import com.kivojenko.spring.forge.jpa.model.relation.EndpointRelation;
import com.kivojenko.spring.forge.jpa.model.relation.EndpointRelationFactory;
import lombok.experimental.SuperBuilder;

@SuperBuilder
public class OneToManyEndpointRelationFactory extends EndpointRelationFactory {
  protected String mappedBy;

  @Override
  public EndpointRelation getReadRelation() {
    return ReadOneToManyEndpointRelation
        .builder()
        .path(path)
        .field(field)
        .entityModel(entityModel)
        .targetEntityModel(targetEntityModel)
        .build();
  }

  @Override
  public EndpointRelation getAddNewRelation() {
    return AddNewOneToManyEndpointRelation
        .builder()
        .path(path)
        .field(field)
        .entityModel(entityModel)
        .targetEntityModel(targetEntityModel)
        .mappedBy(mappedBy)
        .build();
  }

  @Override
  public EndpointRelation getUnlinkRelation() {
    return UnlinkOneToManyEndpointRelation
        .builder()
        .path(path)
        .field(field)
        .entityModel(entityModel)
        .targetEntityModel(targetEntityModel)
        .mappedBy(mappedBy)
        .build();
  }
}
