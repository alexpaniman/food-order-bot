plugins {
    // Apply the Kotlin JVM plugin to add support for Kotlin.
    id("org.jetbrains.kotlin.jvm") version "1.3.41"

    // Apply the application plugin to add support for running project on heroku
    application
}

repositories {
    // Use jcenter for resolving dependencies.
    jcenter()
}

dependencies {
    // Align versions of all Kotlin components
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))

    // Use the Kotlin JDK 8 standard library.
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    // Use the library for building telegram bots
    implementation("org.telegram:telegrambots:4.4.0.1")

    // Use the exposed library for DB usage
    implementation("org.jetbrains.exposed:exposed:0.17.3")

    // Use the postgresql DB as main DB
    implementation("org.postgresql:postgresql:42.2.6")

    // Use the h2 database for testing
    testImplementation("com.h2database:h2:1.4.199")

    // Use the mockito library for testing
    testImplementation("org.mockito:mockito-all:1.10.19")

    // Use the Kotlin test library.
    testImplementation("org.jetbrains.kotlin:kotlin-test")

    // Use the Kotlin JUnit integration.
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit")
}

application {
    // Define the main class for the application
    mainClassName = "org.order.BotLauncherKt"
}

task("stage") {
    dependsOn("installDist")
}