import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.plugins.JavaPlugin
import org.gradle.external.javadoc.StandardJavadocDocletOptions

tasks.register<Javadoc>("aggregateJavadoc") {
    destinationDir = file("$rootDir/docs/javadoc")

    val opts = options as StandardJavadocDocletOptions
    opts.encoding = "UTF-8"
    opts.addBooleanOption("html5", true)
    opts.links("https://docs.oracle.com/en/java/javase/21/docs/api/")

    subprojects {
        plugins.withType<JavaPlugin> {
            val sourceSets = the<SourceSetContainer>()
            source += sourceSets["main"].allJava
            classpath += sourceSets["main"].compileClasspath
        }
    }
}


allprojects {
    group = "com.kivojenko.spring.forge"
    version = "0.1.0"

    repositories {
        mavenCentral()
    }
}