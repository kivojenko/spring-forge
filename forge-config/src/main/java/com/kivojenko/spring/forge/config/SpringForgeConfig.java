package com.kivojenko.spring.forge.config;

import lombok.Data;
import lombok.SneakyThrows;
import org.yaml.snakeyaml.Yaml;

import javax.annotation.processing.ProcessingEnvironment;
import javax.tools.Diagnostic;
import javax.tools.StandardLocation;
import java.io.FileInputStream;
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
    public static String repositoryPackage;

    /**
     * The base package for generated services.
     */
    public static String servicePackage;

    /**
     * The base package for generated controllers.
     */
    public static String controllerPackage;

    /**
     * The base package for generated filters.
     */
    public static String filterPackage;

    /**
     * Indicates whether the configuration has been loaded.
     */
    public static boolean isLoaded = false;

    /**
     * Loads the configuration using the provided processing environment.
     * Configuration is first attempted to be loaded from {@code src/main/resources/springforge.yml},
     * and then merged with or overridden by compiler options prefixed with {@code springforge.}.
     *
     * @param env the processing environment
     */
    public static void load(ProcessingEnvironment env) {
        var yaml = loadYaml(env);

        repositoryPackage = fromYaml(yaml, "repository.package");
        servicePackage = fromYaml(yaml, "service.package");
        controllerPackage = fromYaml(yaml, "controller.package");
        filterPackage = fromYaml(yaml, "filter.package");
        isLoaded = true;
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

        } catch (Exception e) {
            env.getMessager().printMessage(
                    Diagnostic.Kind.WARNING,
                    "Failed to read springforge.yml: " + e.getClass().getName() + ": " + e.getMessage()
            );

            return Map.of();
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
}
