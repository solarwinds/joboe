import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("com.gradleup.shadow")
}

description = "core"

val shadowElements: Configuration by configurations.creating {
    isCanBeConsumed = true
    isCanBeResolved = false
    outgoing.artifact(tasks.named("shadowJar"))
}

dependencies {
    implementation(project(":logging"))
    implementation(project(":config"))
    implementation(project(path = ":sampling", configuration = "shadowElements"))

    api("io.grpc:grpc-netty:1.79.0")
    implementation("io.grpc:grpc-stub:1.79.0")
    implementation("io.grpc:grpc-protobuf:1.79.0")

    compileOnly("org.json:json:20251224")
    compileOnly("io.opentelemetry:opentelemetry-api:1.59.0")
    compileOnly("io.opentelemetry:opentelemetry-context:1.59.0")
    
    implementation("javax.xml.bind:jaxb-api:2.3.1")
    implementation("com.google.code.findbugs:jsr305:3.0.2")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    
    implementation("com.solarwinds:apm-proto:1.0.8") {
        exclude(group = "com.google.guava", module = "guava")
        exclude(group = "io.grpc")
    }

    compileOnly("com.google.auto.service:auto-service:1.1.1")
    annotationProcessor("com.google.auto.service:auto-service:1.1.1")

    testImplementation("org.json:json:20251224")
    testImplementation("io.opentelemetry:opentelemetry-api:1.59.0")
    testImplementation("io.opentelemetry:opentelemetry-context:1.59.0")
}

sourceSets {
    test {
        resources {
            srcDir("src/test/java")
            include("**/*.json")
        }
    }
}

tasks {
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
        
        minimize {
            exclude(dependency("io.grpc:.*"))
            exclude(dependency("io.netty:.*"))
            exclude(dependency("com.google.protobuf:.*"))
        }

        relocate("com.solarwinds.trace", "com.solarwinds.joboe.shaded.trace")
        relocate("android.annotation", "com.solarwinds.joboe.shaded.android.annotation")
        relocate("javax.annotation", "com.solarwinds.joboe.shaded.javax.annotation")
        relocate("cloud", "com.solarwinds.joboe.shaded.cloud")
        relocate("okhttp3", "com.solarwinds.joboe.shaded.okhttp3")
        relocate("okio", "com.solarwinds.joboe.shaded.okio")
        relocate("kotlin", "com.solarwinds.joboe.shaded.kotlin")
        relocate("io", "com.solarwinds.joboe.shaded.io"){
            exclude("io.opentelemetry.**")
        }
        relocate("org", "com.solarwinds.joboe.shaded.org"){
            exclude("org.json.**")
        }
        relocate("google", "com.solarwinds.joboe.shaded.google2")
        relocate("com.google", "com.solarwinds.joboe.shaded.google")
        relocate("javax.xml", "com.solarwinds.joboe.shaded.javax.xml")

        exclude("META-INF/maven/com.google.guava/**")
        exclude("NOTICE")
        exclude("DEPENDENCIES")
        exclude("LICENSE")
        exclude("*.txt")
    }
}