import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    // Apply the Kotlin JVM plugin to add support for Kotlin.
    id("org.jetbrains.kotlin.jvm") version "2.0.20"

    // Apply the shadow jar plugin for creating fat jar
    id("com.github.johnrengelman.shadow") version "8.1.1"

    // Apply the application plugin to add support for running project on heroku
    application
}

repositories {
    // Use jcenter for resolving dependencies.
    mavenCentral()
}

dependencies {
    // Align versions of all Kotlin components
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))

    // Use the Kotlin JDK 8 standard library.
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    // Use the library for building telegram bots
    implementation("org.telegram:telegrambots:6.9.7.1")

    // Use the exposed library for DB usage
    implementation("org.jetbrains.exposed:exposed:0.17.14")

    // Use the postgresql DB as main DB
    implementation("org.postgresql:postgresql:42.2.6")

    // Use google library for phone validation
    implementation("com.googlecode.libphonenumber:libphonenumber:8.10.22")

    // ----------- testing -----------

    // Use the h2 database for testing
    // implementation("com.h2database:h2:1.4.199")

    // Use the mockk library for testing
    implementation("io.mockk:mockk:1.13.11")

    implementation("joda-time:joda-time:2.13.0")

    // Library for building text tables
    implementation("com.jakewharton.picnic:picnic:0.2.0")

    // Fro building PDF
    implementation("com.itextpdf:kernel:7.1.10")

    implementation("com.itextpdf:layout:7.1.10")
    // -------------------------------

    // Use the Kotlin test library.
    testImplementation("org.jetbrains.kotlin:kotlin-test")

    // Use the Kotlin JUnit integration.
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit")
}

application {
    // Define the main class for the application
    mainClass.set("org.order.FoodOrderBotTesterKt")
}

tasks.withType<ShadowJar> {
    manifest {
        attributes["Main-Class"] = "org.order.FoodOrderBotTesterKt"
    }

    archiveFileName.set("food-order-bot-emulated.jar");
}

task("stage") {
    dependsOn("installShadowDist", "installDist")
}

// val compileKotlin: KotlinCompile by tasks
// compileKotlin.kotlinOptions {
    // languageVersion = "2.0.20"
// }
