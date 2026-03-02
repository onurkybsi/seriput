plugins {
    id("seriput-common")
}

dependencies {
    implementation(project(":common"))
    implementation(project(":server"))
    implementation("org.hdrhistogram:HdrHistogram:2.1.12")

    testImplementation("com.google.guava:guava:${Versions.GUAVA}")
    testImplementation("org.awaitility:awaitility:${Versions.AWAITILITY}")
    testImplementation("org.mockito:mockito-core:${Versions.MOCKITO}")
    testImplementation("org.mockito:mockito-junit-jupiter:${Versions.MOCKITO}")
}
