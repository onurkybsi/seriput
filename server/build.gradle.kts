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
    testImplementation("org.awaitility:awaitility:${Versions.AWAITILITY}")
    testImplementation("org.mockito:mockito-core:${Versions.MOCKITO}")
    testImplementation("org.mockito:mockito-junit-jupiter:${Versions.MOCKITO}")
}
