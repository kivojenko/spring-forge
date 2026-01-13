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

@Data
public final class SpringForgeConfig {

    private final String repositoryPackage;
    private final String servicePackage;
    private final String controllerPackage;


    public static SpringForgeConfig load(ProcessingEnvironment env) {
        var yaml = loadYaml(env);
        var options = env.getOptions();

        return new SpringForgeConfig(firstNonNull(fromYaml(yaml, "repository.package"),
                options.get("springforge.repository.package")),
                firstNonNull(fromYaml(yaml, "service.package"), options.get("springforge.service.package")),
                firstNonNull(fromYaml(yaml, "controller.package"), options.get("springforge.controller.package")));
    }

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


    private static String fromYaml(Map<String, Object> yaml, String path) {
        if (yaml == null || yaml.isEmpty()) return null;

        Object current = yaml;
        for (var key : path.split("\\.")) {
            if (!(current instanceof Map<?, ?> map)) return null;
            current = map.get(key);
        }
        return current instanceof String s ? s : null;
    }

    private static String firstNonNull(String... values) {
        for (var v : values) {
            if (v != null && !v.isBlank()) return v;
        }
        return null;
    }
}
