plugins {
    id("seriput-common")
}

java { toolchain { languageVersion = JavaLanguageVersion.of(17) } }

dependencies {
    implementation(project(":common"))

    compileOnly("org.projectlombok:lombok:${Versions.LOMBOK}")
    annotationProcessor("org.projectlombok:lombok:${Versions.LOMBOK}")

    testImplementation("org.mockito:mockito-core:${Versions.MOCKITO}")
    testImplementation("org.mockito:mockito-junit-jupiter:${Versions.MOCKITO}")
    testImplementation("org.awaitility:awaitility:${Versions.AWAITILITY}")
    testImplementation("org.testcontainers:testcontainers:${Versions.TESTCONTAINERS}")
    testImplementation("org.testcontainers:junit-jupiter:${Versions.TESTCONTAINERS}")
}
