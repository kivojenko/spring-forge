package com.kivojenko.spring.forge.annotation.filter;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Configures a filter family declared by {@code @FilterField(family = ...)} on this entity's fields.
 *
 * <p>The annotation is optional: a family that is not declared here combines its members with
 * {@link FamilyMatchMode#OR}. Use it to say otherwise — {@link FamilyMatchMode#AND} makes every supplied
 * member of the family have to match, and {@link FamilyMatchMode#EXISTS} additionally requires at least
 * one of the family's target fields to hold a value. The family as a whole is always combined with
 * {@code AND} against the filters outside it.
 *
 * <p>It is repeatable, so one entity can configure several families, and it is inherited: a family
 * declared on a {@code @MappedSuperclass} or entity superclass applies to its subclasses unless they
 * declare the same name again.
 *
 * <p>Naming a family no field belongs to has no effect.
 *
 * @see FilterField#family()
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
@Repeatable(FilterFamilies.class)
public @interface FilterFamily {
  /**
   * The family name, as used by {@code @FilterField(family = ...)}.
   *
   * @return the family name
   */
  String name();

  /**
   * How the filters of this family are combined with each other.
   *
   * @return the family match mode
   */
  FamilyMatchMode matchMode() default FamilyMatchMode.OR;
}
