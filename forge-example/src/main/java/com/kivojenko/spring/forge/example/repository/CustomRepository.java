package com.kivojenko.spring.forge.example.repository;

public interface CustomRepository {
    default String customMethod() {
        return "custom";
    }
}
