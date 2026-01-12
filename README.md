# Spring Forge

Spring Forge is a **compile-time code generation toolkit** for Spring Boot applications.
It uses **annotation processing (APT)** to generate repositories, REST controllers and
other boilerplate **before your application starts**.

---

## What Spring Forge Does

### @WithJpaRepository

For

```java

@Entity
@WithJpaRepository
public class Person {
    @Id
    Long id;
}
```

Spring Forge generates **at compile time**:

```java
public interface PersonForgeRepository extends JpaRepository<Person, Long> {
}
```

---

### @WithService

For

```java

@Entity
@WithService
public class Person {
    @Id
    Long id;
}
```

Spring Forge generates **at compile time**:

```java

@Service
public class PersonForgeService extends ForgeService<Person, Long, PersonForgeRepository> {
}
```

If `@WithService` is used along with `@WithRestController`, the generated controller will use the service instead of the
repository:

```java

@RestController
@RequestMapping("/person")
public class PersonForgeController extends ForgeControllerWithService<Person, Long, PersonForgeRepository, PersonForgeService> {
}
```

---

### @GetOrCreate

If an entity implements `HasName`, you can use `@GetOrCreate` to generate a `getOrCreate(String name)` method in the
service.

```java

@Entity
@GetOrCreate
public class Person implements HasName {
    @Id
    Long id;
    String name;
}
```

This will automatically enable `@WithService` and generate:

```java

@Service
public class PersonForgeService extends HasNameForgeServiceWithGetOrCreate<Person, Long, PersonForgeRepository> {
    @Override
    protected Person create(String name) {
        return Person.builder().name(name).build(); // Uses Lombok @Builder or empty constructor + setter
    }
}
```

### @WithRestController

For

```java

@Entity
@WithRestController
public class Person {
    @Id
    Long id;
}
```

Spring Forge generates **at compile time**:

```java
public interface PersonRepository extends JpaRepository<Person, Long> {
}
```

```java

@RestController
@RequestMapping("/person")
public class PersonForgeController extends ForgeController<Person, Long, PersonForgeRepository> {
}
```

---

### Generated Endpoints

path = decapitalized entity name + "s"

| Method |              Path               |             Description              |
|:------:|:-------------------------------:|:------------------------------------:|
|  GET   | /{path}?page={page}&size={size} | Paged entities - params are optional |
|  POST  |             /{path}             |         Create a new entity          |
|  GET   |          /{path}/{id}           |           Get entity by ID           |
|  PUT   |          /{path}/{id}           |         Update entity by ID          |
| DELETE |          /{path}/{id}           |         Delete entity by ID          |
|  GET   |          /{path}/count          |        Get total entity count        |

---

## Optional Traits

Spring Forge can extend generated repositories based on **marker interfaces** implemented by your entities.

### HasName

```java
public interface HasName {
    String getName();

    void setName(String name);
}
```

If an entity implements `HasName`:

```java

@Entity
@WithJpaRepository
public class Person implements HasName {
    @Id
    Long id;
    String name;
}
```

The generated repository will also extend:

```java
public interface HasNameRepository<E> {
    boolean existsByName(String name);

    boolean existsByNameIgnoreCase(String name);

    Optional<E> findByName(String name);

    Optional<E> findByNameIgnoreCase(String name);

    Iterable<E> findAllByNameContaining(String name);

    Iterable<E> findAllByNameContainingIgnoreCase(String name, Pageable pageable);
}
```

Resulting in:

```java
public interface PersonForgeRepository extends JpaRepository<Person, Long>, HasNameRepository<Person> {
}
```

Generated controller will accept optional `name` query parameters: `GET /entity?page={page}&size={size}&name={name}`.
Spring Data will generate the query implementations automatically.


---

### Repository reuse (no collisions)

If a repository already exists **in the configured repository package**,
Spring Forge will **reuse it instead of generating a new one**.

---

## Global configuration (recommended)

Spring Forge is configured **at compile time** via `gradle.properties`.

### gradle.properties

```properties
springforge.repository.package=com.example.repository
springforge.service.package=com.example.service
springforge.controller.package=com.example.controller
```

---

### Gradle – Kotlin DSL (`build.gradle.kts`)

```kotlin
tasks.withType<JavaCompile>().configureEach {
    val repoPackage = project.findProperty("springforge.repository.package") as String?
    val servicePackage = project.findProperty("springforge.service.package") as String?
    val controllerPackage = project.findProperty("springforge.controller.package") as String?

    if (!repoPackage.isNullOrBlank()) {
        options.compilerArgs.add("-Aspringforge.repository.package=$repoPackage")
    }

    if (!servicePackage.isNullOrBlank()) {
        options.compilerArgs.add("-Aspringforge.service.package=$servicePackage")
    }

    if (!controllerPackage.isNullOrBlank()) {
        options.compilerArgs.add("-Aspringforge.controller.package=$controllerPackage")
    }
}
```

---

### Gradle – Groovy DSL (`build.gradle`)

```groovy
tasks.withType(JavaCompile).configureEach {
    def repoPackage = project.findProperty("springforge.repository.package")
    def servicePackage = project.findProperty("springforge.service.package")
    def controllerPackage = project.findProperty("springforge.controller.package")

    if (repoPackage) {
        options.compilerArgs << "-Aspringforge.repository.package=${repoPackage}"
    }

    if (servicePackage) {
        options.compilerArgs << "-Aspringforge.service.package=${servicePackage}"
    }

    if (controllerPackage) {
        options.compilerArgs << "-Aspringforge.controller.package=${controllerPackage}"
    }
}
```

## Design philosophy

> If code can be generated deterministically at compile time,
> it should be generated.

Spring Forge trades flexibility for **clarity, safety, and maintainability**.

