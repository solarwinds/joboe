import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("com.gradleup.shadow")
}

description = "sampling"

dependencies {
    implementation(project(":logging"))
    compileOnly("io.opentelemetry:opentelemetry-api:1.59.0")
    implementation("com.github.ben-manes.caffeine:caffeine:2.9.3")

    testImplementation("io.opentelemetry:opentelemetry-api:1.59.0")
}


tasks{
    jar {
        enabled = false
    }

    assemble {
        dependsOn(shadowJar)
    }

    withType<ShadowJar>().configureEach {
        archiveClassifier.set("")
        mergeServiceFiles()
        filesMatching("META-INF/services/**") {
            duplicatesStrategy = DuplicatesStrategy.INCLUDE
        }

        relocate("com.github.benmanes.caffeine", "com.solarwinds.joboe.sampling.shaded.caffeine")
        relocate("org.checkerframework", "com.solarwinds.joboe.sampling.shaded.checkerframework")
        relocate("com.google.errorprone", "com.solarwinds.joboe.sampling.shaded.errorprone")

        exclude("META-INF/*.SF")
        exclude("META-INF/*.DSA")
        exclude("META-INF/*.RSA")
    }
}

val shadowElements: Configuration by configurations.creating {
    isCanBeConsumed = true
    isCanBeResolved = false
    outgoing.artifact(tasks.named("shadowJar"))
}