# Spring Forge

**Spring Forge** is a **compile-time code generation toolkit** for Spring applications.  
It generates **Spring Data repositories (and later services, controllers, tests)** directly from your domain model using **Java annotation processing**.

No reflection.  
No runtime proxies.  
No Spring magic.

If it compiles, it exists.

---

## What Spring Forge Does (Today)

Given this entity:

```java
@Entity
@HasJpaRepository
public class Person {
    @Id
    Long id;
}
```

Spring Forge generates **at compile time**:

```java
public interface PersonRepository extends JpaRepository<Person, Long> {}
```

Spring Data picks it up automatically — exactly as if you had written it by hand.

---

## Optional Domain Traits

Spring Forge can extend generated repositories based on **marker interfaces** implemented by your entities.

### Example: `HasName`

```java
public interface HasName {
    String getName();
    void setName(String name);
}
```

If an entity implements `HasName`:

```java
@Entity
@HasJpaRepository
public class Person implements HasName {
    @Id Long id;
    String name;
}
```

The generated repository will also extend:

```java
public interface HasNameRepository<E> {
    boolean existsByName(String name);
    Optional<E> findByName(String name);
    Optional<E> findByNameIgnoreCase(String name);
}
```

Resulting in:

```java
public interface PersonRepository extends JpaRepository<Person, Long>, HasNameRepository<Person> {}
```

Spring Data will generate the query implementations automatically.

---

## Modules

Spring Forge is split into **clear, purpose-driven modules**:

```
spring-forge
├─ forge-annotations   // public annotations (API)
├─ forge-jpa           // JPA contracts + generators
└─ forge-processor     // annotation processor (compiler logic)
```

### `forge-annotations`
Public annotations used by application code.

- `@HasJpaRepository`

### `forge-jpa`
JPA-specific **contracts and generation logic**.

- `HasName` (entity trait)
- `HasNameRepository` (repository extension)
- `JpaEntityModel`
- `JpaRepositoryGenerator`

### `forge-processor`
The **annotation processor**.

- Scans annotations
- Builds models
- Delegates generation
- Writes source files

No Spring runtime dependencies live here.

---

## Requirements

- **Java 21**
- **Spring Boot 3.x or 4.x**
- **Spring Data JPA**
- Gradle or Maven (Gradle recommended)

---

## Using Spring Forge in a Project

### Gradle (Kotlin DSL)

```kotlin
dependencies {
    compileOnly("com.kivojenko.spring.forge:forge-annotations")
    implementation("com.kivojenko.spring.forge:forge-jpa")
    annotationProcessor("com.kivojenko.spring.forge:forge-processor")
}
```

---

## Verifying Generation

After a clean build:

```bash
./gradlew clean compileJava
```

Generated sources will appear under:

```
build/generated/sources/annotationProcessor/java/main
```

---

## Design Principles

- Compile-time only
- Explicit, boring generated code
- No runtime magic
- Fail fast on misuse


