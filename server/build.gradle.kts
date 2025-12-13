plugins {
    id("seriput-common")
    id("application")
}

application {
    mainClass = "io.seriput.server.Main"
}

dependencies {
    implementation(project(":common"))
}
