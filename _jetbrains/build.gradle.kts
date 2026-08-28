import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.gradle.testing.jacoco.tasks.JacocoReport
import org.gradle.testing.jacoco.plugins.JacocoTaskExtension


// Gradle build plugins: These are tools that help Gradle build the project
// They are NOT IntelliJ plugins or Alethia plugins
plugins {
    // Kotlin JVM complier: this is what elts us compile Kotlin source files to JVM bytecode
    id("org.jetbrains.kotlin.jvm")
    // IntelliJ Platform Gradle Plugin: Handles sandbox IDE download, runIde, and build tasks
    id("org.jetbrains.intellij.platform")
    // Changelog pluglin
    id("org.jetbrains.changelog")
    // JaCoCo: Java Code Coverage library, generates coverage reports from test runs
    jacoco
}

dependencies {
    // JUnit 4: Test framework used for all Alethia Unit Tests
    testImplementation("junit:junit:4.13.2")
    // JSON library for object JSON creation and parsing
    implementation("com.google.code.gson:gson:2.10.1")

    // SLF4J
    implementation("org.slf4j:slf4j-api:2.0.18")
    implementation("org.slf4j:slf4j-jdk14:2.0.18")

    // IntelliJ Platform Gradle Plugin Dependencies Extension
    // Defines which IDE to download for the sandbox
    // and what bundled plugins are required at runtime.
    intellijPlatform {
        // IDE version (e. Target is IntelliJ IDEA Community 2025.1)
        intellijIdeaCommunity("2025.1")
        // IntelliJ Platform test framework: required for repo BasePlatformTestCase
        testFramework(TestFrameworkType.Platform)
        // Git4Idea: bundled git plugin, required fro repo detection, push/commit listeners
        bundledPlugin("Git4Idea")
    }
}

// Configure the test task to generate JaCoCo coverage reports after tests are completed
tasks.named<Test>("test") {
    // Automatically run the jacocoTestReport task
    finalizedBy(tasks.named("jacocoTestReport"))
    extensions.configure<JacocoTaskExtension> {
        isIncludeNoLocationClasses = true
        excludes = listOf("jdk.internal.*")
    }
}

// Configure the JaCoCo test report
// Create an HTML coverage report at build/reports/jacoco/test/html/index.html
tasks.named<JacocoReport>("jacocoTestReport") {
    // Ensure tests always run before report is generated
    dependsOn(tasks.named("test"))
    reports {
        // Generate an HTML report
        html.required.set(true)
    }
}
