description = "config"

dependencies {
    api(project(":logging"))
    compileOnly("org.json:json:20250517")
    testImplementation("org.json:json:20250517")
}
