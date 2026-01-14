package com.kivojenko.spring.forge.config;

import lombok.Data;
import lombok.SneakyThrows;
import org.yaml.snakeyaml.Yaml;

import javax.annotation.processing.ProcessingEnvironment;
import javax.tools.Diagnostic;
import javax.tools.StandardLocation;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.util.Map;

/**
 * Configuration for Spring Forge, loaded from {@code springforge.yml} or processing options.
 */
@Data
public final class SpringForgeConfig {

    /**
     * The base package for generated repositories.
     */
    private final String repositoryPackage;

    /**
     * The base package for generated services.
     */
    private final String servicePackage;

    /**
     * The base package for generated controllers.
     */
    private final String controllerPackage;


    /**
     * Loads the configuration using the provided processing environment.
     * Configuration is first attempted to be loaded from {@code src/main/resources/springforge.yml},
     * and then merged with or overridden by compiler options prefixed with {@code springforge.}.
     *
     * @param env the processing environment
     * @return the loaded configuration
     */
    public static SpringForgeConfig load(ProcessingEnvironment env) {
        var yaml = loadYaml(env);
        var options = env.getOptions();

        return new SpringForgeConfig(firstNonNull(fromYaml(yaml, "repository.package"),
                options.get("springforge.repository.package")),
                firstNonNull(fromYaml(yaml, "service.package"), options.get("springforge.service.package")),
                firstNonNull(fromYaml(yaml, "controller.package"), options.get("springforge.controller.package")));
    }

    /**
     * Loads the YAML configuration from the project resources.
     * 
     * @param env the processing environment
     * @return a map representing the YAML content, or an empty map if not found
     */
    @SneakyThrows
    private static Map<String, Object> loadYaml(ProcessingEnvironment env) {
        try {
            var r = env.getFiler().getResource(StandardLocation.CLASS_OUTPUT, "", "doesntmatter").toUri();
            var projectRoot = Path.of(r).getParent().getParent().getParent().getParent().getParent();

            var properties = Path.of(projectRoot.toString(), "src", "main", "resources", "springforge.yml");
            return new Yaml().load(new FileInputStream(properties.toFile()));

        } catch (FileNotFoundException e) {
            return Map.of();
        } catch (Exception e) {
            env.getMessager()
                    .printMessage(Diagnostic.Kind.ERROR,
                            "Failed to read springforge.yml: " + e.getClass().getName() + ": " + e.getMessage());

            throw e;
        }
    }


    /**
     * Retrieves a string value from a nested map based on a dot-separated path.
     * 
     * @param yaml the configuration map
     * @param path the dot-separated path (e.g., "service.package")
     * @return the string value if found and is a string, {@code null} otherwise
     */
    private static String fromYaml(Map<String, Object> yaml, String path) {
        if (yaml == null || yaml.isEmpty()) return null;

        Object current = yaml;
        for (var key : path.split("\\.")) {
            if (!(current instanceof Map<?, ?> map)) return null;
            current = map.get(key);
        }
        return current instanceof String s ? s : null;
    }

    /**
     * Returns the first non-null and non-blank string from the provided values.
     * 
     * @param values the strings to check
     * @return the first non-null/non-blank string, or {@code null} if none found
     */
    private static String firstNonNull(String... values) {
        for (var v : values) {
            if (v != null && !v.isBlank()) return v;
        }
        return null;
    }
}
