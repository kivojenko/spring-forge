tasks.register<Javadoc>("aggregateJavadoc") {
    destinationDir = file("$rootDir/docs/javadoc")

    val opts = options as StandardJavadocDocletOptions
    opts.encoding = "UTF-8"
    opts.addBooleanOption("html5", true)
    opts.links("https://docs.oracle.com/en/java/javase/21/docs/api/")

    subprojects {
        if (name == "forge-example") return@subprojects
        plugins.withType<JavaPlugin> {
            val sourceSets = the<SourceSetContainer>()
            source += sourceSets["main"].allJava
            classpath += sourceSets["main"].compileClasspath
        }
    }
}

plugins {
    id("java")
    id("io.spring.dependency-management") version "1.1.7"
}


allprojects {
    group = "com.kivojenko.spring.forge"
    version = "0.1.14"

    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "maven-publish")
    apply(plugin = "signing")

    apply(plugin = "io.spring.dependency-management")

    dependencyManagement {
        imports {
            mavenBom("org.testcontainers:testcontainers-bom:1.21.4")
            mavenBom("net.javacrumbs.shedlock:shedlock-bom:7.5.0")
        }
        dependencies {
            dependency("org.projectlombok:lombok:1.18.32")
        }
    }
}