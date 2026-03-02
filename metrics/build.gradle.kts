description = "metrics"

dependencies {
    compileOnly("org.json:json:20250517")
    implementation(project(":config"))
    implementation(project(path = ":core", configuration = "shadowElements"))
    implementation(project(":logging"))
    implementation(project(path = ":sampling", configuration = "shadowElements"))
    testImplementation("org.json:json:20250517")
}

sourceSets {
    test {
        resources {
            srcDir("src/test/java")
            include("**/*.json")
        }
    }
}
