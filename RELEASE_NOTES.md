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
