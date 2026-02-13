plugins {
    `java-library`
    id("io.spring.dependency-management") version "1.1.7"
}

dependencyManagement {
    imports {
        mavenBom("com.kivojenko.spring.forge:spring-forge-bom:0.1.9")
    }
}


java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
    withSourcesJar()
    withJavadocJar()
}

tasks.withType<Javadoc>().configureEach {
    (options as StandardJavadocDocletOptions).addStringOption("Xdoclint:none", "-quiet")
}


dependencies {
    compileOnly(":forge-annotations")
    implementation(":forge-runtime")
    annotationProcessor(":forge-processor")

    compileOnly("org.springframework:spring-web:7.0.1")
    compileOnly("org.springframework.data:spring-data-jpa:4.0.1")

    compileOnly("jakarta.validation:jakarta.validation-api:3.0.2")
    compileOnly("jakarta.persistence:jakarta.persistence-api:3.2.0")

    implementation("com.querydsl:querydsl-core:5.1.0")
    implementation("com.querydsl:querydsl-jpa:5.1.0:jakarta")
    annotationProcessor("com.querydsl:querydsl-apt:5.1.0:jakarta")
    annotationProcessor("jakarta.persistence:jakarta.persistence-api:3.2.0")
    annotationProcessor("jakarta.annotation:jakarta.annotation-api:3.0.0")

    compileOnly("org.projectlombok:lombok:1.18.42")
    annotationProcessor("org.projectlombok:lombok:1.18.42")

    testImplementation("com.google.testing.compile:compile-testing:0.23.0")
}
