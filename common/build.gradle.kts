plugins {
    id("seriput-common")
}

java { toolchain { languageVersion = JavaLanguageVersion.of(17) } }

dependencies {
    api(platform("tools.jackson:jackson-bom:${Versions.JACKSON}"))
    api("tools.jackson.core:jackson-databind")

    testImplementation("com.google.guava:guava:${Versions.GUAVA}")
}
