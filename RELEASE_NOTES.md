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
