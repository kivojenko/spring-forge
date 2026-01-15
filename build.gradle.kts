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

subprojects {

    apply(plugin = "java-library")
    apply(plugin = "maven-publish")
    apply(plugin = "signing")

    extensions.configure<JavaPluginExtension> {
        toolchain.languageVersion.set(JavaLanguageVersion.of(21))
        withSourcesJar()
        withJavadocJar()
    }

    tasks.withType<Javadoc>().configureEach {
        (options as StandardJavadocDocletOptions)
            .addStringOption("Xdoclint:none", "-quiet")
    }
}