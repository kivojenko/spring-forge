import org.gradle.api.publish.maven.MavenPublication


plugins {
    `java-library`
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
                name.set("forge-processor")
                description.set("Spring Forge processor")
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
    implementation(project(":forge-config"))
    implementation(project(":forge-annotations"))
    implementation(project(":forge-runtime"))

    implementation("com.squareup:javapoet:1.13.0")


    testImplementation("com.google.testing.compile:compile-testing:0.23.0")
    testImplementation("jakarta.persistence:jakarta.persistence-api")
    testImplementation("org.springframework.data:spring-data-jpa")
    testImplementation("org.springframework:spring-web")
    testImplementation("org.projectlombok:lombok")
    testAnnotationProcessor("org.projectlombok:lombok")
}
