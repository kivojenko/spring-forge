plugins {
    `java-library`
}

dependencies {
    implementation(project(":forge-annotations"))
    implementation(project(":forge-config"))

    implementation("com.squareup:javapoet:1.13.0")

    compileOnly("org.springframework:spring-web:7.0.1")
    compileOnly("org.springframework:spring-context:7.0.1")
    compileOnly("org.springframework.data:spring-data-jpa:4.0.1")

    compileOnly("jakarta.validation:jakarta.validation-api:3.0.2")
    compileOnly("jakarta.persistence:jakarta.persistence-api:3.2.0")

    compileOnly("org.projectlombok:lombok:1.18.42")
    annotationProcessor("org.projectlombok:lombok:1.18.42")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}