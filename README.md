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
public interface PersonForgeRepository extends JpaRepository<Person, Long> {
}
```

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
public class Role implements HasName {
    @Id
    Long id;
    String name;
}
```

This will automatically enable `@WithService` and generate:

```java

@Service
public class RoleForgeService extends HasNameForgeServiceWithGetOrCreate<Role, Long, RoleForgeRepository> {
    @Override
    protected Role create(String name) {
        return Role.builder().name(name).build(); // Uses Lombok @Builder or empty constructor + setter
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
@RequestMapping("/persons")
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
|  HEAD  |          /{path}/{id}           |        Check if entity exists        |
|  PUT   |          /{path}/{id}           |         Update entity by ID          |
| DELETE |          /{path}/{id}           |         Delete entity by ID          |
|  GET   |          /{path}/count          |        Get total entity count        |

---

### Endpoint Annotations

Spring Forge can generate additional endpoints for associations or custom service methods.

#### @WithEndpoints

Used on association fields in entities. It supports the following attributes:

- `read` (default: `true`): Generates a GET endpoint.
- `remove` (default: `false`): Generates a DELETE endpoint (requires `mappedBy`).
- `path`: Custom path for the association (defaults to field name).
- `getMethodName`: Custom name of the getter method in the service (defaults to `get` + CapitalizedFieldName).

```java

@Entity
public class Company {
    @Id
    Long id;

    @OneToMany(mappedBy = "company")
    @WithEndpoints(read = true, remove = true)
    List<Employee> employees;
}
```

This generates:

- `GET /companies/{id}/employees` - returns a list of employees for the company.
- `DELETE /companies/{id}/employees/{employeeId}` - removes an employee from the company (sets the association to
  `null`).

#### @WithGetEndpoint

Used on custom getter methods in service to expose them as GET endpoints.

```java

@RestController
@RequestMapping("/persons")
public class PersonForgeController extends ForgeController<Person, Long, PersonForgeRepository> {

    @WithGetEndpoint
    public List<Person> getRecent() {
        // custom logic
    }
}
```

This generates:

- `GET /people/recent` - returns the list of people from the custom service method.

Attributes:

- `path`: Custom path for the GET endpoint. If empty, it's derived from the method name (e.g., `getRecent` -> `recent`).

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

## Installation

### Dependencies

```kotlin
compileOnly("com.kivojenko.spring.forge:forge-annotations")
implementation("com.kivojenko.spring.forge:forge-runtime")
annotationProcessor("com.kivojenko.spring.forge:forge-processor")
```

## Optional configuration (recommended)

Spring Forge is configured **at compile time** via `resources/springforge.yml`

```yml
repository:
  package: com.example.repository
service:
  package: com.example.service
controller:
  package: com.example.controller

```

## Design philosophy

> If code can be generated deterministically at compile time,
> it should be generated.

Spring Forge trades flexibility for **clarity, safety, and maintainability**.

## Publishing

After publishing via gradle, make a POST request
to `https://ossrh-staging-api.central.sonatype.com/manual/upload/defaultRepository/com.kivojenko` with
Sonatype auth.
