plugins {
    `java-library`
    id("org.springframework.boot") version "4.0.5"
}


java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
    withSourcesJar()
    withJavadocJar()
}

dependencies {
    compileOnly(project(":forge-annotations"))
    implementation(project(":forge-runtime"))
    annotationProcessor(project(":forge-processor"))

    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.0.2")

    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-webmvc")

    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    runtimeOnly("org.postgresql:postgresql")

    compileOnly("jakarta.validation:jakarta.validation-api")
    compileOnly("jakarta.persistence:jakarta.persistence-api")
    annotationProcessor("jakarta.persistence:jakarta.persistence-api")
    annotationProcessor("jakarta.annotation:jakarta.annotation-api")

    implementation("io.github.openfeign.querydsl:querydsl-core:7.1")
    implementation("io.github.openfeign.querydsl:querydsl-jpa:7.1")
    annotationProcessor("io.github.openfeign.querydsl:querydsl-apt:7.1:jpa")

    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    // --- Testing ---
    testImplementation("org.springframework.boot:spring-boot-starter-actuator-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")

    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
    testImplementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.18.3")
}


tasks.test {
    useJUnitPlatform()
}

