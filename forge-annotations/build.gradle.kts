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
        maven {
            name = "ossrh-staging-api"
            url = uri("https://ossrh-staging-api.central.sonatype.com/service/local/staging/deploy/maven2/")
            credentials {
                username = findProperty("ossrhUsername") as String
                password = findProperty("ossrhPassword") as String
            }
        }
    }
    publications {
        register<MavenPublication>("mavenJava") {
            from(components["java"])

            pom {
                name.set("forge-annotations")
                description.set("Spring Forge public annotations API")
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
