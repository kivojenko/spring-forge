package com.kivojenko.spring.forge.example.processor;

import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.JavaFileObjects;
import com.kivojenko.spring.forge.jpa.factory.JpaEntityModelFactory;
import com.kivojenko.spring.forge.processor.ForgeProcessor;
import org.junit.jupiter.api.Test;

/** Collection-element {@code @GetOrCreate} keys the processor cannot support must fail the build with a clear error. */
class GetOrCreateCollectionKeyErrorTest {

  private static final String ELEMENT = """
      package test;

      import jakarta.persistence.*;

      @Entity
      public class Element {
        @Id private String code;
        private String label;
        @ManyToOne private Owner owner;
        public String getCode() { return code; }
      }
      """;

  private static final String HOLDER = """
      package test;

      import jakarta.persistence.*;
      import java.util.Set;

      @Entity
      public class Holder {
        @Id private Long id;
        @ManyToMany private Set<Element> elements;
      }
      """;

  private static Compilation compileOwner(String annotation) {
    var owner = """
        package test;

        import com.kivojenko.spring.forge.annotation.GetOrCreate;
        import com.kivojenko.spring.forge.annotation.WithService;
        import jakarta.persistence.*;
        import java.util.*;

        @Entity
        @WithService
        %s
        public class Owner {
          @Id @GeneratedValue private Long id;
          private String name;
          @ManyToMany private Set<Element> elements;
          @OneToMany(mappedBy = "owner") private Set<Element> ownedElements;
          @ElementCollection private Set<String> synonyms;
          @ElementCollection private Map<String, String> attributes;
          @ManyToOne private Holder holder;
          public Owner() {}
        }
        """.formatted(annotation);
    // Models are cached statically per compilation; each test compiles anew
    JpaEntityModelFactory.clear();
    return javac()
        .withProcessors(new ForgeProcessor())
        .compile(
            JavaFileObjects.forSourceString("test.Owner", owner),
            JavaFileObjects.forSourceString("test.Element", ELEMENT),
            JavaFileObjects.forSourceString("test.Holder", HOLDER)
        );
  }

  private static void assertKeyError(String annotation, String message) {
    var compilation = compileOwner(annotation);
    assertThat(compilation).failed();
    assertThat(compilation).hadErrorContaining(message);
  }

  @Test
  void rejectsMap() {
    assertKeyError("@GetOrCreate(fields = \"attributes\")", "'attributes' is a Map; maps are not supported");
  }

  @Test
  void rejectsEntityElementKeyedByNonIdField() {
    assertKeyError(
        "@GetOrCreate(fields = \"elements.label\")",
        "a collection of entities must be keyed by the element's ID: expected 'elements.code'"
    );
  }

  @Test
  void rejectsCollectionOfEntitiesWithoutLeaf() {
    assertKeyError(
        "@GetOrCreate(fields = {\"name\", \"elements\"})",
        "a collection of entities must be keyed by the element's ID: expected 'elements.code'"
    );
  }

  @Test
  void rejectsPathBeyondBasicCollection() {
    assertKeyError(
        "@GetOrCreate(fields = \"synonyms.bytes\")",
        "'synonyms' holds basic values; the path must end at the collection"
    );
  }

  @Test
  void rejectsInverseSide() {
    assertKeyError(
        "@GetOrCreate(fields = \"ownedElements.code\")",
        "'ownedElements' is the inverse (mappedBy) side of its association"
    );
  }

  @Test
  void rejectsCollectionBehindRelation() {
    assertKeyError(
        "@GetOrCreate(fields = \"holder.elements.code\")",
        "the collection 'elements' is reached through a related entity"
    );
  }
}
