plugins {
    `java-library`
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

dependencies {
    annotationProcessor("org.yaml:snakeyaml:2.5")
    implementation("org.yaml:snakeyaml:2.5")

    annotationProcessor("org.projectlombok:lombok:1.18.42")
    compileOnly("org.projectlombok:lombok:1.18.42")
}