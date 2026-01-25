package com.kivojenko.spring.forge.jpa.contract;

import com.querydsl.core.types.Predicate;

public interface HasToPredicate {
    Predicate toPredicate();
}
