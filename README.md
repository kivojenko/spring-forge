<div align="center">

# Spring Forge

**Compile-time code generation for Spring Boot.**

Annotate a JPA entity — get its repository, service, REST controller and QueryDSL filter
generated *before* the application starts. No reflection, no runtime proxies, no magic at boot.

[![Maven Central](https://img.shields.io/maven-central/v/com.kivojenko.spring.forge/spring-forge-bom?style=flat-square&logo=apachemaven&label=Maven%20Central)](https://central.sonatype.com/artifact/com.kivojenko.spring.forge/spring-forge-bom)
[![Javadoc](https://img.shields.io/badge/Javadoc-docs.kivojenko.com-1f6feb?style=flat-square)](https://docs.kivojenko.com)
[![Java](https://img.shields.io/badge/Java-21-orange?style=flat-square&logo=openjdk)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0-6DB33F?style=flat-square&logo=springboot)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue?style=flat-square)](LICENSE)

</div>

---

## Contents

- [Why](#why)
- [Install](#install)
- [Quick start](#quick-start)
- [Annotations](#annotations)
- [The generated REST API](#the-generated-rest-api)
- [Association endpoints](#association-endpoints)
- [Filtering](#filtering)
- [Extension points](#extension-points)
- [Configuration](#configuration)
- [How it works](#how-it-works)
- [Project layout](#project-layout)
- [Building from source](#building-from-source)
- [License](#license)

---

## Why

A CRUD resource in Spring is three files of boilerplate that never change shape: a repository
interface, a service that wraps it, a controller that wraps that. Spring Forge writes them for you
during `javac`, as ordinary Java source you can read, debug, step through and override.

```java
@Entity
@Table(name = "authors")
@WithRestController
@GetOrCreate
@Getter @Setter @Builder @AllArgsConstructor @RequiredArgsConstructor
public class Author implements HasName {

  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true)
  private String name;

  @OneToMany(mappedBy = "author", cascade = CascadeType.ALL, orphanRemoval = true)
  @WithEndpoints
  @Builder.Default
  private List<Book> books = new ArrayList<>();
}
```

Compiling that entity produces three files in `build/generated/sources/annotationProcessor`:

```
AuthorForgeRepository.java   interface extends JpaRepository<Author, Long>, HasNameRepository<Author>
AuthorForgeService.java      @Service extends ForgeService<Author, Long, AuthorForgeRepository>
AuthorForgeController.java   @RestController @RequestMapping("authors") extends ForgeController<…>
```

…and a live REST resource:

```
GET|POST         /authors                    PATCH   /authors/{id}
GET|HEAD         /authors/{id}               DELETE  /authors/{id}
PUT              /authors/{id}               GET     /authors/count
POST             /authors/get-or-create?name=Ursula
GET|POST         /authors/{id}/books         DELETE  /authors/{id}/books/{bookId}
```

> [!NOTE]
> Everything is generated at compile time. If it does not compile, you find out during the build —
> not on the first request in production.

---

## Install

### Requirements

| | |
|---|---|
| Java | 21+ |
| Spring Boot | 4.0.x |
| Required | `spring-boot-starter-data-jpa`, `spring-boot-starter-webmvc` (for controllers) |
| Required for `@FilterField` | QueryDSL (`querydsl-jpa` + `querydsl-apt`) and Lombok — generated filter DTOs use both |

### Using the BOM

`spring-forge-bom` is a bill of materials: a `pom`-only artifact that pins every Spring Forge module
to one version. Import it once and declare the modules **without versions** — they can never drift
apart, and upgrading is a one-line change.

| Managed by the BOM | Not managed |
|---|---|
| `forge-annotations`, `forge-config`, `forge-processor`, `forge-runtime` | Spring Boot, QueryDSL, Lombok and everything else — those stay under your control |

The three modules land on three *different* configurations, because they play three different roles:

| Module | Configuration | Why |
|---|---|---|
| `forge-annotations` | `compileOnly` / `provided` | `SOURCE` retention — needed to compile, never at runtime |
| `forge-runtime` | `implementation` | Generated classes extend `ForgeService` / `ForgeController` |
| `forge-processor` | `annotationProcessor` | Runs during `javac`, not part of your app |

#### Gradle (Kotlin DSL)

```kotlin
dependencies {
    val forgeBom = platform("com.kivojenko.spring.forge:spring-forge-bom:0.1.24")
    implementation(forgeBom)
    compileOnly(forgeBom)
    annotationProcessor(forgeBom)

    compileOnly("com.kivojenko.spring.forge:forge-annotations")
    implementation("com.kivojenko.spring.forge:forge-runtime")
    annotationProcessor("com.kivojenko.spring.forge:forge-processor")
}
```

> [!IMPORTANT]
> A `platform()` only constrains the configuration it is added to, and `compileOnly` and
> `annotationProcessor` do not extend `implementation`. Add the BOM to all three, or the versionless
> `forge-annotations` and `forge-processor` declarations will fail to resolve.

<details>
<summary>Gradle with the <code>io.spring.dependency-management</code> plugin</summary>

If your build already applies `io.spring.dependency-management` (Spring Initializr adds it to Gradle
projects), an imported BOM applies to every configuration at once, so one import is enough:

```kotlin
plugins {
    id("io.spring.dependency-management") version "1.1.7"
}

dependencyManagement {
    imports {
        mavenBom("com.kivojenko.spring.forge:spring-forge-bom:0.1.24")
    }
}

dependencies {
    compileOnly("com.kivojenko.spring.forge:forge-annotations")
    implementation("com.kivojenko.spring.forge:forge-runtime")
    annotationProcessor("com.kivojenko.spring.forge:forge-processor")
}
```

</details>

<details>
<summary>Maven</summary>

```xml
<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>com.kivojenko.spring.forge</groupId>
      <artifactId>spring-forge-bom</artifactId>
      <version>0.1.24</version>
      <type>pom</type>
      <scope>import</scope>
    </dependency>
  </dependencies>
</dependencyManagement>

<dependencies>
  <dependency>
    <groupId>com.kivojenko.spring.forge</groupId>
    <artifactId>forge-annotations</artifactId>
    <scope>provided</scope>
  </dependency>
  <dependency>
    <groupId>com.kivojenko.spring.forge</groupId>
    <artifactId>forge-runtime</artifactId>
  </dependency>
</dependencies>

<build>
  <plugins>
    <plugin>
      <groupId>org.apache.maven.plugins</groupId>
      <artifactId>maven-compiler-plugin</artifactId>
      <configuration>
        <annotationProcessorPaths>
          <path>
            <groupId>com.kivojenko.spring.forge</groupId>
            <artifactId>forge-processor</artifactId>
            <version>0.1.24</version>
          </path>
        </annotationProcessorPaths>
      </configuration>
    </plugin>
  </plugins>
</build>
```

> [!NOTE]
> `annotationProcessorPaths` is resolved by `maven-compiler-plugin`, separately from
> `<dependencies>`. Keep the explicit `<version>` there, or use `maven-compiler-plugin` 3.12.0+,
> which can take it from `<dependencyManagement>`.

</details>

### A complete build file

<details>
<summary>build.gradle.kts for a Spring Boot 4 application using Spring Forge</summary>

```kotlin
plugins {
    java
    id("org.springframework.boot") version "4.0.6"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com.example"
version = "0.0.1-SNAPSHOT"

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

repositories {
    mavenCentral()
}

dependencies {
    // --- Spring Forge, versions from the BOM ---
    val forgeBom = platform("com.kivojenko.spring.forge:spring-forge-bom:0.1.24")
    implementation(forgeBom)
    compileOnly(forgeBom)
    annotationProcessor(forgeBom)

    compileOnly("com.kivojenko.spring.forge:forge-annotations")
    implementation("com.kivojenko.spring.forge:forge-runtime")
    annotationProcessor("com.kivojenko.spring.forge:forge-processor")

    // --- Spring ---
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    runtimeOnly("org.postgresql:postgresql")

    // --- Required for @FilterField ---
    implementation("io.github.openfeign.querydsl:querydsl-core:7.1")
    implementation("io.github.openfeign.querydsl:querydsl-jpa:7.1")
    annotationProcessor("io.github.openfeign.querydsl:querydsl-apt:7.1:jpa")

    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    // --- Entity metadata for the processor ---
    annotationProcessor("jakarta.persistence:jakarta.persistence-api")
    annotationProcessor("jakarta.annotation:jakarta.annotation-api")
}

tasks.test {
    useJUnitPlatform()
}
```

</details>

### Without the BOM

Perfectly fine — just pin each module yourself and keep the versions identical:

```kotlin
dependencies {
    compileOnly("com.kivojenko.spring.forge:forge-annotations:0.1.24")
    implementation("com.kivojenko.spring.forge:forge-runtime:0.1.24")
    annotationProcessor("com.kivojenko.spring.forge:forge-processor:0.1.24")
}
```

A complete, runnable setup lives in [`forge-example`](forge-example).

---

## Quick start

**1. Annotate an entity.**

```java
@Entity
@WithRestController
public class Person {
  @Id @GeneratedValue
  private Long id;
  private String name;
}
```

**2. Build.** `@WithRestController` implies a service, which implies a repository, so all three are
generated next to the entity (or into the packages you configure):

```java
public interface PersonForgeRepository extends JpaRepository<Person, Long> {}

@Service
public class PersonForgeService extends ForgeService<Person, Long, PersonForgeRepository> {}

@RestController
@RequestMapping("persons")
public class PersonForgeController
    extends ForgeController<Person, Long, PersonForgeRepository, PersonForgeService> {}
```

**3. Call it.** `GET /persons?page=0&size=20`, `POST /persons`, `PATCH /persons/1`, …

> [!TIP]
> The base path defaults to the pluralised, decapitalised entity name: `Person → persons`,
> `Category → categories`. A name already ending in `s` is left alone (`Address → address`).
> Override it with `@WithRestController(path = "people")`.

---

## Annotations

### Trigger annotations

Each one implies the layers below it, so you only ever annotate for the topmost layer you want.

| Annotation | Generates | Implies |
|---|---|---|
| `@WithJpaRepository` | repository | — |
| `@WithService` | service | repository |
| `@GetOrCreate` | `getOrCreate` method (+ endpoint if a controller exists) | service, repository |
| `@WithRestController` | controller | service, repository |

<details>
<summary>Attributes</summary>

**`@WithJpaRepository`**

| Attribute | Default | Description |
|---|---|---|
| `packageName` | `""` | Sub-package appended to the base package for generated types |
| `makeAbstract` | `false` | Mark the generated repository interface `abstract` |
| `interfaces` | `{}` | Extra interfaces the repository should extend — see [generic repository interfaces](#generic-repository-interfaces) |

**`@WithService`**

| Attribute | Default | Description |
|---|---|---|
| `packageName` | `""` | Sub-package appended to the base package |
| `makeAbstract` | `false` | Mark the service `abstract` and omit `@Service`, so a subclass becomes the bean |

**`@WithRestController`**

| Attribute | Default | Description |
|---|---|---|
| `path` | pluralised entity name | Base path of the resource |
| `packageName` | `""` | Sub-package appended to the base package |
| `makeAbstract` | `false` | Mark the controller `abstract` and omit `@RestController`/`@RequestMapping`, so a subclass can map it |
| `allowSlashes` | `false` | Allow slashes in default endpoints by id (e.g. `/{*id}`) |

**`@GetOrCreate`**

| Attribute | Default | Description |
|---|---|---|
| `field` | `"name"` | Lookup/creation field. Supports nested paths such as `"country.code"` |
| `path` | `"/get-or-create"` | Endpoint path, relative to the resource |
| `ignoreCase` | `true` | Case-insensitive lookup for `String` fields; ignored for other types |

> [!IMPORTANT]
> `packageName` is a *sub-package*, appended to the configured (or entity's own) base package.
> The first non-blank `packageName` among the three trigger annotations applies to all generated types.

</details>

### `@GetOrCreate` in detail

```java
@Entity
@WithRestController
@GetOrCreate                      // defaults to the "name" field; entity implements HasName
public class Role implements HasName { … }
```

```java
// generated in RoleForgeService
@Transactional
public Role getOrCreate(String name) {
  return repository.findByNameIgnoreCase(name).orElseGet(() -> createSafely(name));
}

protected Role create(String name) {
  return Role.builder().name(name).build();   // Lombok @Builder, or ctor + setter
}
```

`createSafely` catches `DataIntegrityViolationException` and re-reads, so concurrent callers converge
on a single row instead of blowing up. Any field works, including a nested one:

```java
@GetOrCreate(field = "sku")            // POST /products/get-or-create?sku=SKU-1
@GetOrCreate(field = "country.code")   // POST /offices/get-or-create?country.code=EE
```

### `@WithEndpoints`

Placed on an association field to expose that relation over REST. See
[association endpoints](#association-endpoints) for the exact routes.

| Attribute | Default | Description |
|---|---|---|
| `path` | field name | Path segment for the association |
| `read` | `true` | `GET` the associated entity/collection |
| `addNew` | `true` | `POST` a brand-new entity into the association |
| `linkExisting` | `true` | `PUT` an existing entity into the association (`@ManyToOne`, `@ManyToMany`) |
| `remove` | `true` | `DELETE` the link |

### `@WithGetEndpoint`

Placed on a **public entity method returning a generic collection**, it exposes that method per
instance:

```java
@WithGetEndpoint
@JsonIgnore
public List<String> getBooksTitles() {
  return books.stream().map(Book::getTitle).toList();
}
```

```java
// generated in AuthorForgeController
@GetMapping("/{id}/booksTitles")
public Iterable<String> getBooksTitles(@PathVariable Long id) {
  return getById(id).getBooksTitles();
}
```

Methods returning a single, non-generic value are supported too; the endpoint then returns that
type directly:

```java
@WithGetEndpoint
@JsonIgnore
public Integer getBooksCount() {
  return books.size();
}
// → @GetMapping("/{id}/booksCount") public Integer getBooksCount(@PathVariable Long id)
```

The path is `path()` when set, otherwise the method name with a leading `get` stripped and
decapitalised.

---

## The generated REST API

For a resource mapped at `/{path}`:

| Method | Path | Success | Description |
|:---|:---|:---:|:---|
| `GET` | `/{path}` | 200 | Paged list — accepts `page`, `size`, `sort` and every filter parameter |
| `POST` | `/{path}` | 201 | Create |
| `GET` | `/{path}/{id}` | 200 | Read one |
| `HEAD` | `/{path}/{id}` | 200 | Existence check |
| `PUT` | `/{path}/{id}` | 201 | Full update |
| `PATCH` | `/{path}/{id}` | 200 | Partial update from a JSON object |
| `DELETE` | `/{path}/{id}` | 204 | Delete |
| `GET` | `/{path}/count` | 200 | Total row count |
| `POST` | `/{path}/get-or-create` | 200 | Only with `@GetOrCreate` |

`GET /{path}` returns a Spring `Page`, so the payload is `{ "content": [...], "totalElements": …, … }`.
The default page size comes from [`getAll.page.size`](#configuration) and is unbounded unless you set it.

> [!WARNING]
> Spring Forge does not register a `@ControllerAdvice`. `getById` on a missing row throws
> `EntityNotFoundException`, which Spring renders as a 500 unless you map it yourself.

### PATCH semantics

`PATCH` takes a partial JSON object and merges it into the managed entity:

- values are deserialised with Jackson using each field's generic type, so nested objects and
  generic collections work;
- collections are merged **in place** (`clear()` + `addAll()`), keeping Hibernate's collection
  management and `orphanRemoval` intact;
- the [`fixPatch`](#service-hooks) hook runs on the merged entity just before saving.

---

## Association endpoints

`@WithEndpoints` generates a different route set per relation type. `{path}` is the resource base
path, `{sub}` the association path, `{subId}` the related entity's id.

| Relation | `read` | `addNew` | `linkExisting` | `remove` |
|---|---|---|---|---|
| `@OneToOne` | `GET /{path}/{id}/{sub}` | `POST /{path}/{id}/{sub}` | — | `DELETE /{path}/{id}/{sub}` |
| `@Embedded` | `GET /{path}/{id}/{sub}` | `POST /{path}/{id}/{sub}` | — | `DELETE /{path}/{id}/{sub}` |
| `@ManyToOne` | `GET /{path}/{id}/{sub}` | `POST /{path}/{id}/{sub}` | `PUT /{path}/{id}/{sub}/{subId}` | `DELETE /{path}/{id}/{sub}/{subId}` |
| `@OneToMany(mappedBy)` | `GET /{path}/{id}/{sub}` | `POST /{path}/{id}/{sub}` | — | `DELETE /{path}/{id}/{sub}/{subId}` |
| `@ManyToMany` | `GET /{path}/{id}/{sub}` | `POST /{path}/{id}/{sub}` | `PUT /{path}/{id}/{sub}/{subId}` | `DELETE /{path}/{id}/{sub}/{subId}` |

`POST` creates and links a new entity from the request body; `PUT` links one that already exists;
`DELETE` unlinks (it does not delete the target row unless JPA cascades say so).

<details>
<summary>Examples</summary>

```java
@Entity
public class Book {
  @Id Long id;

  @ManyToOne
  @WithEndpoints
  Author author;

  @ManyToMany
  @WithEndpoints
  List<Category> categories;
}
```

```
GET    /books/{id}/author                    read the author
POST   /books/{id}/author                    create an author and link it
PUT    /books/{id}/author/{authorId}         link an existing author
DELETE /books/{id}/author/{authorId}         unlink

GET    /books/{id}/categories                list categories
POST   /books/{id}/categories                create a category and link it
PUT    /books/{id}/categories/{categoryId}   link an existing category
DELETE /books/{id}/categories/{categoryId}   unlink
```

```java
@Entity
public class Author {
  @Id Long id;

  @OneToMany(mappedBy = "author")
  @WithEndpoints
  List<Book> books;
}
```

```
GET    /authors/{id}/books                   list this author's books
POST   /authors/{id}/books                   create a book owned by this author
DELETE /authors/{id}/books/{bookId}          unlink the book (sets its author to null)
```

</details>

---

## Filtering

`@FilterField` on entity fields generates a filter DTO plus a QueryDSL predicate, and binds it to
`GET /{path}` as query parameters.

```java
@Entity
@WithRestController
public class Product {
  @Id @GeneratedValue Long id;

  @FilterField(stringMatchMode = StringMatchMode.CONTAINS)
  private String name;

  @FilterField(name = "manufacturer")
  private String brand;

  @FilterField(orNull = true)
  private String description;

  @FilterField                                   // EXACT_OR_RANGE by default
  private BigDecimal price;

  @FilterField
  private ProductType type;                      // enum

  @ManyToOne
  @FilterField(targetField = "name", stringMatchMode = StringMatchMode.CONTAINS_IGNORE_CASE)
  private ProductCategory category;

  @ManyToMany
  @FilterField(iterableMatchMode = IterableMatchMode.ANY)
  private Set<Tag> tags = new HashSet<>();
}
```

```java
public class ProductForgeFilter implements HasToPredicate {
  private String name;
  private String manufacturer;
  private String description;
  private BigDecimal price;
  private BigDecimal minPrice;
  private BigDecimal maxPrice;
  private Set<ProductType> types = new HashSet<>();
  private String category;
  private Set<Long> tags = new HashSet<>();

  public BooleanBuilder toPredicate() { … }
}
```

```
GET /products?name=phone&minPrice=100&maxPrice=900&types=ELECTRONICS&category=lectr&tags=1&tags=2
```

The repository also gains matching `findBy…` derived queries, and extends
`QuerydslPredicateExecutor<E>` so the predicate can be paged. A derived query is named after the
property it actually reads, so `targetField` shows up in the method name — `findByCategory_Name(String)`.
Filters that cannot be a derived query at all (collection targets, `IterableMatchMode.AMOUNT`) are
served only by `toPredicate()`.

### Parameter naming

The DTO field name *is* the query parameter name, and it is not always the entity field name:

| Entity field | Generated parameter(s) |
|---|---|
| `String`, `Boolean`, `UUID`, … | same name — `description` |
| number/date, `EXACT` | `price` |
| number/date, `RANGE` | `minPrice`, `maxPrice` |
| number/date, `EXACT_OR_RANGE` *(default)* | all three |
| enum | pluralised set — `type` → `types` |
| single association, no `targetField` | pluralised set of **ids** — `country` → `countries` |
| collection association, no `targetField` | set of **ids**, name unchanged — `tags` |
| any association **with** `targetField` | scalar of the target's type, **name unchanged** — `category` |
| collection of scalars / `@ElementCollection` | scalar of the element type, **name unchanged** — `keywords` |
| any field with `isPresent = true` | `Boolean` named `has<Field>` — `description` → `hasDescription` |
| `@DiscriminatorColumn` | `List` named after the column — `vehicle_type` → `vehicleType` |

> [!CAUTION]
> Unknown query parameters are silently ignored by Spring's data binding, so a mistyped filter name
> returns *unfiltered* results rather than an error. `category` and `categories` are not
> interchangeable — check the generated DTO when a filter looks like it is doing nothing.

`name` overrides the parameter name explicitly:

```java
@FilterField(name = "manufacturer")
private String brand;              // ?manufacturer=Acme
```

### Match modes

| Attribute | Values | Default |
|---|---|---|
| `stringMatchMode` | `EQUALS`, `EQUALS_IGNORE_CASE`, `CONTAINS`, `CONTAINS_IGNORE_CASE`, `STARTS_WITH`, `ENDS_WITH` | `CONTAINS` |
| `comparisonMatchMode` | `EXACT`, `RANGE`, `EXACT_OR_RANGE` | `EXACT_OR_RANGE` |
| `iterableMatchMode` | `ANY`, `ALL` | `ANY` |
| `minBoundMode` / `maxBoundMode` | `INCLUDES`, `EXCLUDES` | `INCLUDES` |

Other attributes:

| Attribute | Default | Description |
|---|---|---|
| `name` | field name | Query-parameter / DTO field name |
| `targetField` | `""` | Filter on a field *of* the association or `@Embedded` value (`category.name`, `dye.colorIndex`), or an absolute path from the root entity when placed on a transient field |
| `required` | `false` | Adds `@NotNull` (`@NotBlank` for `String`) to the DTO; the controller validates with `@Valid` |
| `orNull` | `false` | Also match rows where the column is `NULL` |
| `isPresent` | `false` | Presence filter named `has<Field>` unless `name` is set: `true` → `isNotNull()` (`isNotEmpty()` for collections), `false` → `isNull()` / `isEmpty()` |
| `family` | `""` | Groups this filter with others of the same family; members are OR-ed with each other (`@FilterFamily` on the entity can make it `AND` or `EXISTS`), the family AND-ed with the rest |

```java
@FilterField(orNull = true)
@FilterField(isPresent = true)
private String description;        // ?description=… and ?hasDescription=true

@Embedded
@FilterField(targetField = "colorIndex", isPresent = true)
private Dye dye;                   // ?hasDye=true → dye.colorIndex is not null
```

### Families

Filters are AND-ed together by default. `family` groups a set of them so they are OR-ed with each other
instead, and the group as a whole is AND-ed with everything outside it:

```java
@FilterField(family = "search", stringMatchMode = StringMatchMode.CONTAINS_IGNORE_CASE)
private String title;

@FilterField(family = "search", stringMatchMode = StringMatchMode.CONTAINS_IGNORE_CASE)
private String summary;

@ElementCollection
@FilterField(family = "search", stringMatchMode = StringMatchMode.CONTAINS_IGNORE_CASE)
private List<String> keywords;

@FilterField
private Boolean published;
```

```
GET /articles?title=quantum&summary=quantum&keywords=quantum&published=true
```

matches published articles whose title **or** summary **or** any keyword contains `quantum`:

```java
var __familySearch = new BooleanBuilder();
if (title != null && !title.isBlank()) {
  __familySearch.or(entity.title.containsIgnoreCase(title));
}
if (summary != null && !summary.isBlank()) {
  __familySearch.or(entity.summary.containsIgnoreCase(summary));
}
if (keywords != null && !keywords.isBlank()) { … __familySearch.or(… exists …); }
if (__familySearch.hasValue()) {
  builder.and(__familySearch);
}
if (published != null) {
  builder.and(entity.published.eq(published));
}
```

Every member keeps its own parameter, type and match modes — only the way the predicates are combined
changes. Parameters that are not sent contribute nothing, so sending a single member filters by that
member alone, and a family whose members are all absent leaves the query untouched. Any filter kind can
join a family, including `targetField` paths, collection targets, presence filters and the discriminator
filter; a family with one member behaves exactly like an ungrouped filter.

`@FilterFamily` on the entity switches a family to `AND`, so every member that *is* sent has to match.
It is optional — an undeclared family is `OR`:

```java
@Entity
@WithRestController
@FilterFamily(name = "attribution", matchMode = FamilyMatchMode.AND)
public class Article {

  @FilterField(family = "attribution", stringMatchMode = StringMatchMode.CONTAINS_IGNORE_CASE)
  private String author;

  @FilterField(family = "attribution", stringMatchMode = StringMatchMode.CONTAINS_IGNORE_CASE)
  private String section;
  …
}
```

```
GET /articles?author=ada&section=science      # author AND section, not either
```

| Attribute | Default | Description |
|---|---|---|
| `name` | — | The family name, as used by `@FilterField(family = …)` |
| `matchMode` | `OR` | `OR` — any member may match; `AND` — every member that is sent must match; `EXISTS` — see below |

`FamilyMatchMode.EXISTS` goes one step further: at least one of the family's target fields has to *hold a
value*, whether or not any of its parameters is sent. Each member contributes an `isNotNull()` check
(`isNotEmpty()` for a collection), those are OR-ed and required, and every supplied member's own predicate
is AND-ed on top:

```java
@Entity
@WithRestController
// only contacts that can actually be reached are ever listed
@FilterFamily(name = "reachability", matchMode = FamilyMatchMode.EXISTS)
public class Contact {

  @FilterField(family = "reachability", stringMatchMode = StringMatchMode.CONTAINS_IGNORE_CASE)
  private String email;

  @FilterField(family = "reachability", stringMatchMode = StringMatchMode.CONTAINS_IGNORE_CASE)
  private String phone;

  @ElementCollection
  @FilterField(family = "reachability", stringMatchMode = StringMatchMode.CONTAINS_IGNORE_CASE)
  private List<String> handles = new ArrayList<>();
}
```

```java
var __familyReachability = new BooleanBuilder();
__familyReachability.and(entity.email.isNotNull()
    .or(entity.phone.isNotNull())
    .or(entity.handles.isNotEmpty()));
if (email != null && !email.isBlank()) {
  __familyReachability.and(entity.email.containsIgnoreCase(email));
}
…
```

```
GET /contacts                       # every contact with an email, a phone or a handle
GET /contacts?email=example.com     # …of those, the ones whose email matches
```

> [!IMPORTANT]
> `EXISTS` is the one mode that constrains the query even when no member parameter is sent, so it
> permanently narrows the resource's list endpoint. `OR` and `AND` families leave an untouched query alone.

The annotation is repeatable, so one entity can configure several families, and it is inherited from a
`@MappedSuperclass` or entity superclass unless the subclass declares the same name again. Naming a
family no field belongs to has no effect.

> [!NOTE]
> A family only affects `toPredicate()`. The derived `findBy…` queries are per-field and keep AND
> semantics, so an OR search has to go through the filter DTO.

### Nested and collection paths

`targetField` resolves relative to the annotated association, using QueryDSL `.any()` for
collections:

```java
@ManyToOne
@FilterField(targetField = "name")        // → entity.category.name
private ProductCategory category;

@OneToMany(mappedBy = "ingredient")
@FilterField(targetField = "name")        // → entity.alternativeNames.any().name
private Set<IngredientAlternativeName> alternativeNames;
```

When the **target itself** is a collection of scalars — an `@ElementCollection`, whether reached through
`targetField` or annotated directly — the filter takes a single value of the *element* type and matches it
against every element. `any()` cannot be serialised for a collection nested inside an `@Embedded` value, so
these compile to an `exists` subquery instead:

```java
@Embedded
@FilterField(targetField = "hexColors")   // ?dye=#FF — exists(… from dye.hexColors h where h like '#FF%')
private Dye dye;                          // Dye holds @ElementCollection List<String> hexColors

@ElementCollection
@FilterField                              // ?keywords=warm
private List<String> keywords;
```

`stringMatchMode` / `comparisonMatchMode` apply to each element; `isPresent` still asks about the
collection itself (`isNotEmpty()` / `isEmpty()`), and `orNull` also matches rows whose collection is empty.

### Inheritance

For a `@DiscriminatorColumn` hierarchy, a discriminator filter is added automatically — all three
`DiscriminatorType`s are supported, and matching uses QueryDSL `instanceOf` against the mapping:

```java
// VehicleForgeFilter
private List<String> vehicleType;   // ?vehicleType=CAR&vehicleType=TRUCK
```

---

## Extension points

### Reuse over generation

If a repository, service or controller with the expected name already exists in the target package,
Spring Forge **skips generation and uses yours**. That is the escape hatch: write the class by hand,
extend `ForgeService`/`ForgeController` yourself, and the rest of the pipeline still wires up.

### `HasName`

Implementing `HasName` opts an entity into name-based queries:

```java
public interface HasName {
  String getName();
  void setName(String name);
}
```

The generated repository extends `HasNameRepository<E>`:

```java
boolean       existsByName(String name);
boolean       existsByNameIgnoreCase(String name);
Optional<E>   findByName(String name);
Optional<E>   findByNameIgnoreCase(String name);
List<E>       findAllByNameContaining(String name);
List<E>       findAllByNameContainingIgnoreCase(String name, Pageable pageable);
```

The generated service also rejects duplicate names on `create`, and `@GetOrCreate` defaults to this
field.

### Generic repository interfaces

An interface passed to `interfaces` is automatically parameterised with the entity type when it
declares exactly one type parameter:

```java
public interface WithNameTranslation<T> {
  Optional<T> findByNameEnUS(String name);
}

@Entity
@WithJpaRepository(interfaces = WithNameTranslation.class)
public class Organism { … }
```

```java
public interface OrganismForgeRepository
    extends JpaRepository<Organism, Long>, WithNameTranslation<Organism> {}
```

### Service hooks

Override these on a hand-written service that extends the generated one:

| Hook | When |
|---|---|
| `E fixParameters(E entity)` | before `create` and `PUT` update |
| `E fixPatch(E entity)` | after `PATCH` fields are merged into the managed entity, before save |

`fixPatch` receives the *managed* instance with changes already applied — adjust it in place
(back-references, de-duplicated values); returning a different instance would discard the merged
collections.

### `ForgePersistenceAspect`

AOP hooks around the generated persistence calls. Extend it, implement `entityType()`, mark it
`@Component`:

```java
@Component
@Slf4j
public class AuditLogger extends ForgePersistenceAspect<Product> {

  @Override
  protected Class<Product> entityType() {
    return Product.class;
  }

  @Override
  public void afterCreate(Product product) {
    log.info("Created {}", product);
  }
}
```

| Hook | Fires on |
|---|---|
| `beforeCreate(E)` / `afterCreate(E)` | create |
| `beforeUpdate(E)` / `afterUpdate(E)` | `PUT` update — `afterUpdate` also fires after `PATCH` |
| `beforeDelete(E)` / `afterDelete(E)` | delete |
| `beforeAdd(Object main, Object sub)` / `afterAdd(…)` | association add/link |
| `beforeDelete(Object main, Object sub)` / `afterDelete(…)` | association unlink |

> [!NOTE]
> `PATCH` has no "before" hook: `ForgeService.patch(ID, Map)` never receives the entity, only its id
> and a field map, so there is nothing for AOP to hand you.

---

## Configuration

Compile-time configuration lives in `src/main/resources/springforge.yml` of the **consuming**
project. It is read by the annotation processor, not by Spring at runtime.

```yml
repository:
  package: com.example.repository
service:
  package: com.example.service
controller:
  package: com.example.controller
filter:
  package: com.example.filter
getAll:
  page:
    size: 100
```

| Key | Default | Effect |
|---|---|---|
| `repository.package` | entity's package | Base package for generated repositories |
| `service.package` | entity's package | Base package for generated services |
| `controller.package` | entity's package | Base package for generated controllers |
| `filter.package` | entity's package | Base package for generated filters |
| `getAll.page.size` | `Integer.MAX_VALUE` | `@PageableDefault` size on `GET /{path}` |

Per-entity `packageName` attributes are appended to these as a sub-package.

---

## How it works

1. **Discovery** — `ForgeProcessor` collects every type annotated with a Spring Forge annotation.
2. **Graph expansion** — relations reachable through `@WithEndpoints` are followed, so related
   entities get modelled too.
3. **Modelling** — `JpaEntityModelFactory` builds a `JpaEntityModel` per entity: id, packages,
   requirements, relations, filter fields.
4. **Generation** — `Filter`, `Repository`, `Service` and `Controller` generators emit source with
   [JavaPoet](https://github.com/square/javapoet).
5. **Skip what exists** — anything already present in the target package is left alone.

Generated classes extend the runtime base classes (`ForgeService`, `ForgeController`,
`ForgeAbstractController`), so the behaviour lives in a versioned library while the generated code
stays thin and readable.

> **Design philosophy**
> If code can be generated deterministically at compile time, it should be generated.
> Spring Forge trades flexibility for clarity, safety and maintainability.

---

## Project layout

| Module | What it is |
|---|---|
| [`forge-annotations`](forge-annotations) | The public annotation API — `compileOnly` for consumers |
| [`forge-config`](forge-config) | Compile-time `springforge.yml` loading |
| [`forge-processor`](forge-processor) | The `javax.annotation.processing.Processor` entry point |
| [`forge-runtime`](forge-runtime) | Base classes for generated code, plus the generators and models |
| [`forge-bom`](forge-bom) | Bill of materials |
| [`forge-example`](forge-example) | Runnable sample app and the integration test suite |

---

## Building from source

```bash
./gradlew build
```

Tests in `forge-example` are integration tests against a real PostgreSQL instance via
[Testcontainers](https://testcontainers.com/), so a running Docker daemon is required:

```bash
./gradlew test
```

Aggregate Javadoc for every module (published to [docs.kivojenko.com](https://docs.kivojenko.com)):

```bash
./gradlew aggregateJavadoc
```

<details>
<summary>Publishing</summary>

Publish with Gradle, then promote the staged repository:

```bash
curl -X POST -u "$OSSRH_USER:$OSSRH_PASS" \
  https://ossrh-staging-api.central.sonatype.com/manual/upload/defaultRepository/com.kivojenko
```

</details>

---

## License

[Apache License 2.0](LICENSE)
