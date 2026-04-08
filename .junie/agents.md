### Spring Forge Project Overview

Spring Forge is a compile-time code generation toolkit for Spring Boot applications, utilizing Java Annotation Processing (APT) to generate repositories, services, and REST controllers.

#### Core Concepts
- **Annotation-Driven**: Generation is triggered by annotations like `@WithJpaRepository`, `@WithService`, `@WithRestController`, and `@GetOrCreate`.
- **Compile-Time**: All code is generated before the application starts, ensuring type safety and reducing runtime overhead.
- **Pluggable Architecture**: Uses "Generators" (JavaPoet) to produce code and "Models" to represent entities and their relationships.

#### Module Breakdown
- `forge-annotations`: Contains all public annotations used to trigger code generation.
- `forge-config`: Handles compile-time configuration via `springforge.yml` or processing options.
- `forge-processor`: The main `javax.annotation.processing.Processor` implementation. It scans for annotations and orchestrates the generation.
- `forge-runtime`: Provides the base classes (`ForgeService`, `ForgeController`) and the generator logic (`ControllerGenerator`, `ServiceGenerator`, etc.). Generated code extends these base classes.
- `forge-bom`: Bill of Materials for managing versions of Spring Forge modules.
- `forge-example`: A sample project demonstrating integration and usage.

#### Key Technologies
- **Java Annotation Processing (APT)**: The core engine for scanning source code and generating new files.
- **JavaPoet**: Used for programmatic generation of Java source files.
- **Spring Data JPA**: The target for generated repositories.
- **Spring Web**: The target for generated REST controllers.
- **QueryDSL**: Used for generating filters and executing complex queries if `@FilterField` is used.
- **Lombok**: Extensively used in both the library and generated code for boilerplate reduction.

#### How It Works (The Workflow)
1. **Discovery**: `ForgeProcessor` scans for classes annotated with Spring Forge annotations.
2. **Modeling**: `JpaEntityModelFactory` creates a `JpaEntityModel` for each discovered entity, resolving relationships and requirements.
3. **Graph Expansion**: The processor expands the graph by following relationships (e.g., `@WithEndpoints` on fields) to ensure all related entities are modeled.
4. **Generation**: Specific generators (`RepositoryGenerator`, `ServiceGenerator`, etc.) use JavaPoet to create the corresponding Java files.
5. **Base Classes**: Generated classes typically extend base classes from `forge-runtime` (e.g., `ForgeService`), which provide standard CRUD implementations.

#### Common Patterns & Extension Points
- **Persistence Hooks**: Implement `ForgePersistenceHook<E>` to intercept create, update, and delete operations.
- **Trait Interfaces**: Implement `HasName` to automatically get name-based search methods in repositories and services.
- **Custom Endpoints**: Use `@WithEndpoints` on entity fields for association-specific REST endpoints, or `@WithGetEndpoint` on service methods.
- **Filtering**: Use `@FilterField` on entity fields to generate a `Filter` class and enable QueryDSL-based paged searching.
- **Manual Overrides**: If a repository, service, or controller already exists in the configured package, the processor will skip generation for that component, allowing manual customization.

#### Development & Testing
- **Integration Tests**: See `forge-example` for how to test the generated code in a Spring Boot environment.
- **Configuration**: `src/main/resources/springforge.yml` is used at *compile time* of the project consuming the library.
