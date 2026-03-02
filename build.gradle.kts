import com.diffplug.gradle.spotless.SpotlessExtension
import org.gradle.kotlin.dsl.provideDelegate

plugins {
    `java-library`
    id("com.diffplug.spotless") version "6.25.0" apply false
    id("io.freefair.lombok") version "8.13" apply false
    id("com.gradleup.shadow") version "8.3.9" apply false
}

val projectVersion = System.getProperty("releaseVersion") ?: property("version").toString()
rootProject.extra["projectVersion"] = projectVersion

allprojects {
    val projectVersion: String by rootProject.extra
    group = "com.solarwinds.joboe"
    version = projectVersion
}

subprojects {
    apply(plugin = "java-library")
    apply(plugin = "io.freefair.lombok")
    apply(plugin = "com.diffplug.spotless")
    apply(plugin = "maven-publish")

    configure<JavaPluginExtension> {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(8))
        }
        withSourcesJar()
    }

    dependencies {
        testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.2")
        testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.2")
        testImplementation("org.junit-pioneer:junit-pioneer:1.9.1")
        testImplementation("org.mockito:mockito-core:3.12.4")
        testImplementation("org.mockito:mockito-junit-jupiter:3.12.4")
        testImplementation("org.mockito:mockito-inline:3.12.4")
    }

    tasks {
        withType<Test>().configureEach {
            useJUnitPlatform()
            testLogging {
                events("passed", "skipped", "failed")
            }
        }
    }

    configure<SpotlessExtension> {
        java {
            googleJavaFormat()
            removeUnusedImports()
            trimTrailingWhitespace()
            endWithNewline()
        }
    }
    
    configure<PublishingExtension> {
        repositories {
            maven {
                name = "GitHubPackages"
                url = uri("https://maven.pkg.github.com/solarwinds/joboe")
                credentials {
                    username = System.getenv("GITHUB_USERNAME")
                    password = System.getenv("GITHUB_TOKEN")
                }
            }
        }
    }

    afterEvaluate {
        configure<PublishingExtension> {
            publications {
                create<MavenPublication>("maven") {
                    val variant = if (plugins.hasPlugin("com.gradleup.shadow")) "shadow" else "java"
                    from(components[variant])
                }
            }
        }
    }
}
