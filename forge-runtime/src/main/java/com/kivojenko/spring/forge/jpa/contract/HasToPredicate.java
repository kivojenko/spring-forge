package com.kivojenko.spring.forge.jpa.contract;

import com.querydsl.core.types.Predicate;

/**
 * Interface for objects that can be converted to a QueryDSL {@link Predicate}.
 * Typically implemented by generated filter classes.
 */
public interface HasToPredicate {
    /**
     * Converts this object to a {@link Predicate}.
     *
     * @return the QueryDSL predicate
     */
    Predicate toPredicate();
}
