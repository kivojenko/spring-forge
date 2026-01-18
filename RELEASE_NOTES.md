### Release Notes - Version 0.1.6 (2026-01-19)

This release introduces the `@FilterField` generation logic.

#### New Features

- **Generic Filter Generation**: `@FilterField` generates more generic filter classes. For associations, it automatically uses the ID type of the related entity, facilitating filtering by foreign keys.

#### Installation

Update the version in your `build.gradle.kts`:

```kotlin
implementation(platform("com.kivojenko.spring.forge:spring-forge-bom:0.1.6"))
```

---

### Release Notes - Version 0.1.5 (2026-01-18)

This release introduces support for `@ManyToOne` associations in `@WithEndpoints` and restructures internal relation handling.

#### New Features

- **Many-to-One Association Support**: `@WithEndpoints` now supports `@ManyToOne` associations, allowing for:
    - **Read**: Fetch the associated entity.
    - **Add**: Link an existing entity to the association.
    - **Remove**: Unlink (set to null) the associated entity.
- **Improved Association Handling**: Internal refactoring of how associations (One-to-Many and Many-to-One) are handled during code generation, leading to more robust and consistent endpoint generation.

#### Improvements & Internal Changes

- **Relation Model Restructuring**: Endpoint relation classes have been moved to specialized packages (`manyToOne` and `oneToMany`) and now inherit from a common `ServiceRepositoryEndpointRelation` base class.
- **Enhanced Code Generation**: Improved the logic for generating repository, service, and controller methods for associations, including better parameter handling and transaction management.

#### Installation

Update the version in your `build.gradle.kts`:

```kotlin
implementation(platform("com.kivojenko.spring.forge:spring-forge-bom:0.1.5"))
```

---

### Release Notes - Version 0.1.4 (2026-01-17)

This release focuses on internal refactoring for better annotation handling and improved REST controller path generation.

#### New Features

- **Custom REST Path Support**: The `@WithRestController` annotation now supports a `path` attribute to specify a custom base path for the generated controller.
- **Improved Pluralization**: REST controller paths now handle entities ending in "y" more correctly (e.g., `Category` -> `/categories`).

#### Improvements & Internal Changes

- **Annotation-Driven Requirements**: Refactored `JpaEntityRequirements` to store actual annotation instances. This improves the robustness of the generation logic and simplifies internal checks.
- **Code Generation Cleanup**: Improved formatting and consistency in the generated Java code across repositories, services, and controllers.
- **Enhanced Type Safety**: Better internal handling of entity types and IDs during the code generation process.

#### Installation

Update the version in your `build.gradle.kts`:

```kotlin
implementation(platform("com.kivojenko.spring.forge:spring-forge-bom:0.1.4"))
```

---

### Release Notes - Version 0.1.3 (2026-01-17)

This release introduces the ability to generate abstract repositories and services, and fixes an issue where `@RequestMapping` was added to abstract controllers.

#### New Features

- **Abstract Repositories**: Added `makeAbstract` attribute to `@WithJpaRepository`. When set to `true`, the generated repository will be abstract.
- **Abstract Services**: Added `makeAbstract` attribute to `@WithService`. When set to `true`, the generated service will be abstract and won't have the `@Service` annotation, allowing for custom implementations while keeping the generated CRUD logic.

#### Improvements & Bug Fixes

- **Controller Generation**: Fixed a bug where abstract controllers (generated with `@WithRestController(makeAbstract = true)`) were incorrectly annotated with `@RestController` and `@RequestMapping`. These annotations are now only applied to non-abstract (implemented) controllers.
- **Service Generation**: Fixed a bug where in entities with non-string type name services still tried to use string for getOrCreate
- **Requirements Model**: Updated internal `JpaEntityRequirements` to properly distinguish between abstract and implemented states for repositories, services, and controllers.

#### Installation

Update the version in your `build.gradle.kts`:

```kotlin
implementation(platform("com.kivojenko.spring.forge:spring-forge-bom:0.1.3"))
```

---

### Release Notes - Version 0.1.2 (2026-01-15)

This release introduces the ability to generate abstract controllers and includes internal refactorings and documentation improvements.

#### New Features

- **Abstract Controllers**: Added `makeAbstract` attribute to `@WithRestController`. When set to `true`, the generated controller will be abstract, allowing developers to extend it and provide custom implementation logic while still benefiting from the generated boilerplate.

#### Improvements & Internal Changes

- **Javadoc Enhancements**: Added missing `@return` tags and other Javadoc improvements to the annotation classes for better IDE support and documentation clarity.
- **Internal Refactoring**: Refactored the internal model structure by renaming `JpaEntityModelRequirements` to `JpaEntityRequirements` for better naming consistency.
- **Processor Reliability**: Improved package scanning in the annotation processor to ensure better entity detection in certain project structures.

#### Installation

Update the version in your `build.gradle.kts`:

```kotlin
implementation(platform("com.kivojenko.spring.forge:spring-forge-bom:0.1.2"))
```

---

### Release Notes - Version 0.1.1 (2026-01-15)

This release introduces the Bill of Materials (BOM) for easier dependency management and includes several improvements and bug fixes.

#### New Features

- **Bill of Materials (BOM)**: Added `spring-forge-bom` to simplify dependency management across multiple modules.
- **Improved Configuration Handling**: The processor now handles missing or malformed `springforge.yml` more gracefully, issuing a warning instead of a compilation error.

#### Improvements & Bug Fixes

- **Repository API Alignment**: Updated `HasNameRepository` to return `List` instead of `Iterable` for better compatibility with common Spring Data JPA patterns and easier usage in services.
- **Documentation**: Refreshed Javadoc documentation across all modules.

#### Installation

Add the BOM to your `build.gradle.kts`:

```kotlin
implementation(platform("com.kivojenko.spring.forge:spring-forge-bom:0.1.1"))
```

Then add the required dependencies without versions:

```kotlin
compileOnly("com.kivojenko.spring.forge:forge-annotations")
implementation("com.kivojenko.spring.forge:forge-runtime")
annotationProcessor("com.kivojenko.spring.forge:forge-processor")
```

---

### Release Notes - Version 0.1.0 (2026-01-15)

It is the first release of **Spring Forge**, an annotation-driven boilerplate generator for Spring Data JPA applications. Spring Forge simplifies development by generating repositories, services, and REST controllers at compile-time based on your entity definitions.

#### Key Features

- **Automated CRUD Generation**: Generate Spring Data JPA Repositories, Services, and REST Controllers with a single annotation.
  - `@WithJpaRepository`: Generates a `JpaRepository` interface.
  - `@WithService`: Generates a service layer with common CRUD operations and a `JpaRepository` interface.
  - `@WithRestController`: Generates a REST controller with standard endpoints and a `JpaRepository` interface.
- **Smart Association Handling**:
  - `@WithEndpoints`: Generate specialized endpoints for collection associations (one-to-many, many-to-many) to read or remove related entities.
- **Protocol-Oriented Extensions**:
  - `HasName` Support: Implementing the `HasName` interface enables specialized search capabilities and "Get or Create" logic via the `@GetOrCreate` annotation.
- **Compile-Time Safety & Performance**: All code is generated using Java Annotation Processing (APT), ensuring zero runtime overhead and immediate feedback during compilation.
- **Flexible Configuration**:
  - Configure target packages for generated code via `springforge.yml` or compiler options.
  - Custom paths and naming for generated endpoints and methods.
- **Repository Reuse**: Automatically detects and reuses existing repositories if they are already present in the configured package, preventing collisions.

#### Installation

Add the following dependencies to your `build.gradle.kts`:

```kotlin
compileOnly("com.kivojenko.spring.forge:forge-annotations:0.1.0")
implementation("com.kivojenko.spring.forge:forge-runtime:0.1.0")
annotationProcessor("com.kivojenko.spring.forge:forge-processor:0.1.0")
```
