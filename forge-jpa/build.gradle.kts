plugins {
    `java-library`
}

dependencies {
    implementation(project(":forge-annotations"))

    implementation("com.squareup:javapoet:1.13.0")

}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}