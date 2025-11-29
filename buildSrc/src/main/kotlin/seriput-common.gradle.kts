plugins {
    id("java")
}

repositories { mavenCentral() }

java { toolchain { languageVersion = JavaLanguageVersion.of(25) } }

dependencies {
    val junitVersion = "6.0.1"
    val assertJVersion = "4.0.0-M1"

    testImplementation(platform("org.junit:junit-bom:$junitVersion"))

    testImplementation("org.assertj:assertj-core:${assertJVersion}")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> { useJUnitPlatform() }
