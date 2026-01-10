# Spring Forge

Spring Forge is a **compile-time code generation toolkit** for Spring Boot applications.
It uses **annotation processing (APT)** to generate repositories, REST controllers, and
other boilerplate **before your application starts**.

No reflection.  
No runtime magic.  
No Spring context hacks.

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
public interface PersonRepository extends JpaRepository<Person, Long> {
}
```

```java

@RestController
@RequestMapping("/person")
public class PersonController extends AbstractController<Person, PersonRepository> {
}
```

Spring Data picks it up automatically — exactly as if you had written it by hand.

---

### Generated Endpoints

| Method |     Path      |                 Description                  |
|:------:|:-------------:|:--------------------------------------------:|
|  GET   |    /entity    | List entities (paginated, iterable response) |
|  POST  |    /entity    |             Create a new entity              |
|  GET   | /entity/{id}  |               Get entity by ID               |
|  PUT   | /entity/{id}  |             Update entity by ID              |
| DELETE | /entity/{id}  |             Delete entity by ID              |
|  GET   | /entity/count |            Get total entity count            |
|  POST  | /entity/paged |       Get paged result (Page<_Entity>)       |

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
    @Id
    Long id;
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
public interface PersonRepository extends JpaRepository<Person, Long>, HasNameRepository<Person> {
}
```

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
springforge.controller.package=com.example.controller
```

---

### Gradle – Kotlin DSL (`build.gradle.kts`)

```kotlin
tasks.withType<JavaCompile>().configureEach {
    val repoPackage = project.findProperty("springforge.repository.package") as String?
    val controllerPackage = project.findProperty("springforge.controller.package") as String?

    if (!repoPackage.isNullOrBlank()) {
        options.compilerArgs.add(
            "-Aspringforge.repository.package=$repoPackage"
        )
    }

    if (!controllerPackage.isNullOrBlank()) {
        options.compilerArgs.add(
            "-Aspringforge.controller.package=$controllerPackage"
        )
    }
}
```

---

### Gradle – Groovy DSL (`build.gradle`)

```groovy
tasks.withType(JavaCompile).configureEach {
    def repoPackage = project.findProperty("springforge.repository.package")
    def controllerPackage = project.findProperty("springforge.controller.package")

    if (repoPackage) {
        options.compilerArgs << "-Aspringforge.repository.package=${repoPackage}"
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

---

## Modules

| Module              | Purpose                                                        |
|---------------------|----------------------------------------------------------------|
| `forge-annotations` | Public annotations (`@HasJpaRepository`, `@HasRestController`) |
| `forge-processor`   | Annotation processor (generation logic)                        |
| `forge-jpa`         | JPA-related models and generators                              |
| `forge-web-api`     | Runtime API (e.g. `AbstractController`)                        |

