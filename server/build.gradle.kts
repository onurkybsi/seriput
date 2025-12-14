plugins {
    id("seriput-common")
    id("application")
}

application {
    mainClass = "io.seriput.server.Main"
}

dependencies {
    implementation(project(":common"))

    testImplementation("com.google.guava:guava:${Versions.GUAVA}")
}
