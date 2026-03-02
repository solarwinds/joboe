description = "metrics"

dependencies {
    compileOnly("org.json:json:20251224")
    implementation(project(":config"))
    implementation(project(path = ":core", configuration = "shadowElements"))
    implementation(project(":logging"))
    implementation(project(path = ":sampling", configuration = "shadowElements"))
    testImplementation("org.json:json:20251224")
}

sourceSets {
    test {
        resources {
            srcDir("src/test/java")
            include("**/*.json")
        }
    }
}
