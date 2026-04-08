import org.gradle.api.publish.maven.MavenPublication


plugins {
    `java-library`
    id("org.springframework.boot") version "4.0.5"
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
    withSourcesJar()
    withJavadocJar()
}

tasks.withType<Javadoc>().configureEach {
    (options as StandardJavadocDocletOptions).addStringOption("Xdoclint:none", "-quiet")
}


publishing {
    repositories {
        val ossrhUser = providers.gradleProperty("ossrhUsername").orNull
        val ossrhPass = providers.gradleProperty("ossrhPassword").orNull

        if (!ossrhUser.isNullOrBlank() && !ossrhPass.isNullOrBlank()) {
            maven {
                name = "ossrh-staging-api"
                url = uri("https://ossrh-staging-api.central.sonatype.com/service/local/staging/deploy/maven2/")
                credentials {
                    username = ossrhUser
                    password = ossrhPass
                }
            }
        }
    }
    publications {
        register<MavenPublication>("mavenJava") {
            from(components["java"])

            pom {
                name.set("forge-runtime")
                description.set("Spring Forge generation runtime support")
                url.set("https://github.com/kivojenko/spring-forge")

                licenses {
                    license {
                        name.set("Apache License 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0")
                    }
                }

                developers {
                    developer {
                        id.set("kivojenko")
                        name.set("Ksenija Kivojenko")
                        url.set("https://github.com/kivojenko")
                        email.set("kivojenko@gmail.com")
                    }
                }

                scm {
                    connection.set("scm:git:git://github.com/kivojenko/spring-forge.git")
                    developerConnection.set("scm:git:ssh://github.com:kivojenko/spring-forge.git")
                    url.set("https://github.com/kivojenko/spring-forge")
                }
            }
        }
    }
}

signing {
    useGpgCmd()
    sign(publishing.publications["mavenJava"])
}


dependencies {
    implementation(project(":forge-annotations"))
    implementation(project(":forge-config"))

    implementation("com.squareup:javapoet:1.13.0")

    compileOnly("org.springframework:spring-web")
    compileOnly("org.springframework.data:spring-data-jpa")

    compileOnly("jakarta.validation:jakarta.validation-api")
    compileOnly("jakarta.persistence:jakarta.persistence-api")

    implementation("io.github.openfeign.querydsl:querydsl-core:7.1")
    implementation("io.github.openfeign.querydsl:querydsl-jpa:7.1")
    annotationProcessor("io.github.openfeign.querydsl:querydsl-apt:7.1")

    annotationProcessor("jakarta.persistence:jakarta.persistence-api")
    annotationProcessor("jakarta.annotation:jakarta.annotation-api")

    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    testImplementation("com.google.testing.compile:compile-testing:0.23.0")
}
