package com.kivojenko.spring.forge.jpa.contract;

/**
 * Interface for entities that have a name property.
 * Used by {@link com.kivojenko.spring.forge.annotation.GetOrCreate} to identify entities
 * for which "get or create" operations can be generated.
 */
public interface HasName {
    /**
     * Gets the name of the entity.
     * @return the name
     */
    String getName();

    /**
     * Sets the name of the entity.
     * @param name the name to set
     */
    void setName(String name);
}
