plugins {
    id("seriput-common")
}

dependencies {
    implementation(project(":common"))
    implementation("org.hdrhistogram:HdrHistogram:2.1.12")

    testImplementation(project(":server"))
}

tasks.register<JavaExec>("runGetBenchmark") {
    group = "benchmark"
    description = "Runs the Seriput GET benchmark."
    classpath = sourceSets["main"].runtimeClasspath
    mainClass = "io.seriput.benchmark.SeriputGetBenchmark"
    workingDir = rootProject.projectDir
}
