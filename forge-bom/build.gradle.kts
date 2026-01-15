plugins {
    `java-platform`
}

javaPlatform {
    allowDependencies()
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
        register<MavenPublication>("mavenBom") {
            from(components["javaPlatform"])

            groupId = "com.kivojenko.spring.forge"
            artifactId = "spring-forge-bom"
            version = project.version.toString()

            pom {
                packaging = "pom"
                name.set("Spring Forge BOM")
                description.set("Bill of Materials for Spring Forge modules")
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
                        email.set("kivojenko@gmail.com")
                        url.set("https://github.com/kivojenko")
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
    sign(publishing.publications["mavenBom"])
}


dependencies {
    constraints {
        api("com.kivojenko.spring.forge:forge-annotations:0.1.1")
        api("com.kivojenko.spring.forge:forge-config:0.1.1")
        api("com.kivojenko.spring.forge:forge-jpa:0.1.1")
        api("com.kivojenko.spring.forge:forge-runtime:0.1.1")
    }
}
