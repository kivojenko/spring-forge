package com.kivojenko.spring.forge.jpa.model.relation;

import com.kivojenko.spring.forge.jpa.model.base.JpaEntityModel;
import lombok.experimental.SuperBuilder;

import javax.lang.model.element.VariableElement;

@SuperBuilder
public abstract class EndpointRelationFactory {
  protected String path;
  protected VariableElement field;
  protected JpaEntityModel entityModel;
  protected JpaEntityModel targetEntityModel;

  public EndpointRelation getReadRelation() {
    return null;
  }

  public EndpointRelation getAddNewRelation() {
    return null;
  }

  public EndpointRelation getLinkExistingRelation() {
    return null;
  }

  public EndpointRelation getUnlinkRelation() {
    return null;
  }
}
