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
