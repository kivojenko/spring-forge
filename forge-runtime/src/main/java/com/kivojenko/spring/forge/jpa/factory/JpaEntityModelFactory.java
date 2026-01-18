package com.kivojenko.spring.forge.jpa.factory;

import com.kivojenko.spring.forge.config.SpringForgeConfig;
import com.kivojenko.spring.forge.jpa.model.base.JpaEntityModel;
import lombok.Setter;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Factory for creating and caching {@link JpaEntityModel} instances.
 */
public final class JpaEntityModelFactory {

    private static final Map<TypeElement, JpaEntityModel> CACHE = new HashMap<>();
    private static SpringForgeConfig config;
    @Setter
    private static ProcessingEnvironment env;

    /**
     * Returns all cached entity models.
     *
     * @return a list of all entity models
     */
    public static List<JpaEntityModel> getAll() {
        return CACHE.values().stream().toList();
    }

    /**
     * Gets or creates the entity model for the given type element.
     *
     * @param entity the entity type element
     * @return the entity model
     */
    public static JpaEntityModel get(TypeElement entity) {
        if (config == null) config = SpringForgeConfig.load(env);

        return CACHE.computeIfAbsent(entity, e -> new JpaEntityModel(e, config, env));
    }
}
