package com.kivojenko.spring.forge.example.model.generics;

import java.util.Optional;

public interface WithNameTranslation<T> {
  Optional<T> findByNameEnUS(String name);
}
