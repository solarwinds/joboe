description = "config"

dependencies {
    api(project(":logging"))
    compileOnly("org.json:json:20251224")
    testImplementation("org.json:json:20251224")
}
