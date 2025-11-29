plugins {
    id("java-library")
}

repositories { mavenCentral() }

java { toolchain { languageVersion = JavaLanguageVersion.of(25) } }

dependencies {
    testImplementation(platform("org.junit:junit-bom:${Versions.JUNIT}"))

    testImplementation("org.assertj:assertj-core:${Versions.ASSERTJ}")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> { useJUnitPlatform() }
