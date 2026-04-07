package com.kivojenko.spring.forge.processor;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.JavaFileObjects;
import com.kivojenko.spring.forge.jpa.factory.JpaEntityModelFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.tools.JavaFileObject;

import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

public class ForgeProcessorTest {

    @BeforeEach
    public void setUp() {
        JpaEntityModelFactory.clear();
    }

    @Test
    public void testBasicGeneration() {
        JavaFileObject entity = JavaFileObjects.forSourceLines(
                "com.example.User",
                "package com.example;",
                "",
                "import com.kivojenko.spring.forge.annotation.WithJpaRepository;",
                "import jakarta.persistence.Entity;",
                "import jakarta.persistence.Id;",
                "",
                "@Entity",
                "@WithJpaRepository",
                "public class User {",
                "    @Id",
                "    private Long id;",
                "    private String name;",
                "}"
        );

        Compilation compilation = javac()
                .withProcessors(new ForgeProcessor())
                .compile(entity);

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile("com.example.UserForgeRepository")
                .hasSourceEquivalentTo(JavaFileObjects.forSourceLines(
                        "com.example.UserForgeRepository",
                        "package com.example;",
                        "",
                        "import java.lang.Long;",
                        "import org.springframework.data.jpa.repository.JpaRepository;",
                        "",
                        "public interface UserForgeRepository extends JpaRepository<User, Long> {",
                        "}"
                ));
    }

    @Test
    public void testServiceGeneration() {
        JavaFileObject entity = JavaFileObjects.forSourceLines(
                "com.example.User",
                "package com.example;",
                "",
                "import com.kivojenko.spring.forge.annotation.WithService;",
                "import jakarta.persistence.Entity;",
                "import jakarta.persistence.Id;",
                "",
                "@Entity",
                "@WithService",
                "public class User {",
                "    @Id",
                "    private Long id;",
                "    public void setId(Long id) {}",
                "}"
        );

        Compilation compilation = javac()
                .withProcessors(new ForgeProcessor())
                .compile(entity);

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile("com.example.UserForgeService")
                .hasSourceEquivalentTo(JavaFileObjects.forSourceLines(
                        "com.example.UserForgeService",
                        "package com.example;",
                        "",
                        "import com.kivojenko.spring.forge.jpa.contract.ForgeService;",
                        "import java.lang.Long;",
                        "import java.lang.Override;",
                        "import org.springframework.stereotype.Service;",
                        "",
                        "@Service",
                        "public class UserForgeService extends ForgeService<User, Long, UserForgeRepository> {",
                        "  /**",
                        "   * Sets the ID of the given {@link User} entity.",
                        "   *",
                        "   * @param entity the entity to update",
                        "   * @param id the new ID",
                        "   */",
                        "  @Override",
                        "  protected void setId(User entity, Long id) {",
                        "    entity.setId(id);",
                        "  }",
                        "}"
                ));
    }
    @Test
    public void testControllerGeneration() {
        JavaFileObject entity = JavaFileObjects.forSourceLines(
                "com.example.User",
                "package com.example;",
                "",
                "import com.kivojenko.spring.forge.annotation.WithRestController;",
                "import jakarta.persistence.Entity;",
                "import jakarta.persistence.Id;",
                "",
                "@Entity",
                "@WithRestController",
                "public class User {",
                "    @Id",
                "    private Long id;",
                "    public void setId(Long id) {}",
                "}"
        );

        Compilation compilation = javac()
                .withProcessors(new ForgeProcessor())
                .compile(entity);

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile("com.example.UserForgeController")
                .hasSourceEquivalentTo(JavaFileObjects.forSourceLines(
                        "com.example.UserForgeController",
                        "package com.example;",
                        "",
                        "import com.kivojenko.spring.forge.jpa.contract.ForgeController;",
                        "import java.lang.Long;",
                        "import org.springframework.data.domain.Page;",
                        "import org.springframework.data.domain.Pageable;",
                        "import org.springframework.data.web.PageableDefault;",
                        "import org.springframework.web.bind.annotation.GetMapping;",
                        "import org.springframework.web.bind.annotation.RequestMapping;",
                        "import org.springframework.web.bind.annotation.RestController;",
                        "",
                        "/**",
                        " * Generated REST controller for {@link User}.",
                        " * Provides endpoints for standard CRUD operations and custom relations.",
                        " */",
                        "@RestController",
                        "@RequestMapping(\"users\")",
                        "public class UserForgeController extends ForgeController<User, Long, UserForgeRepository, UserForgeService> {",
                        "  /**",
                        "   * Retrieves a paged result of all {@link User} entities.",
                        "   *",
                        "   * @param pageable the pagination information",
                        "   * @return a page of entities",
                        "   */",
                        "  @GetMapping",
                        "  public Page<User> findAll(@PageableDefault(size = 2147483647) Pageable pageable) {",
                        "    return service.findAll(pageable);",
                        "  }",
                        "}"
                ));
    }
    @Test
    public void testFilterGeneration() {
        JavaFileObject entity = JavaFileObjects.forSourceLines(
                "com.example.User",
                "package com.example;",
                "",
                "import com.kivojenko.spring.forge.annotation.FilterField;",
                "import com.kivojenko.spring.forge.annotation.WithJpaRepository;",
                "import jakarta.persistence.Entity;",
                "import jakarta.persistence.Id;",
                "",
                "@Entity",
                "@WithJpaRepository",
                "public class User {",
                "    @Id",
                "    private Long id;",
                "    @FilterField",
                "    private String name;",
                "}"
        );
        Compilation compilation = javac()
                .withProcessors(new ForgeProcessor())
                .compile(entity);
        assertThat(compilation).succeeded();
    }
}
