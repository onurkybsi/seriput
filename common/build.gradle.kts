plugins {
    id("seriput-common")
}

dependencies {
    api(platform("tools.jackson:jackson-bom:${Versions.JACKSON}"))
    api("tools.jackson.core:jackson-databind")
    api("tools.jackson.core:jackson-databind")
}
