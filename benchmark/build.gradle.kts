plugins {
    id("seriput-common")
}

dependencies {
    implementation(project(":client"))
    implementation(project(":common"))
    implementation(project(":server"))
    implementation("org.hdrhistogram:HdrHistogram:2.1.12")

    testImplementation("com.google.guava:guava:${Versions.GUAVA}")
    testImplementation("org.awaitility:awaitility:${Versions.AWAITILITY}")
    testImplementation("org.mockito:mockito-core:${Versions.MOCKITO}")
    testImplementation("org.mockito:mockito-junit-jupiter:${Versions.MOCKITO}")
}

registerBenchmarkTask("runGetThroughput", "io.seriput.benchmark.GetThroughputBenchmark")
registerBenchmarkTask("runPutThroughput", "io.seriput.benchmark.PutThroughputBenchmark")
registerBenchmarkTask("runDeleteThroughput", "io.seriput.benchmark.DeleteThroughputBenchmark")

fun registerBenchmarkTask(name: String, mainClassName: String) {
    tasks.register<JavaExec>(name) {
        group = "benchmark"
        classpath = sourceSets["main"].runtimeClasspath
        mainClass.set(mainClassName)
        args = listOfNotNull(
            project.findProperty("concurrency")?.toString(),
            project.findProperty("targetRps")?.toString()
        )
    }
}