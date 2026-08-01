import org.jetbrains.intellij.platform.gradle.TestFrameworkType

// Plugins needed for Gradle to build the project
// NOT plugins for Alethia's use
plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.intellij.platform")
    id("org.jetbrains.changelog")
}

dependencies {
    testImplementation("junit:junit:4.13.2")

    // IntelliJ Platform Gradle Plugin Dependencies Extension
    intellijPlatform {
        intellijIdeaCommunity("2025.1")
        testFramework(TestFrameworkType.Platform)
        bundledPlugin("Git4Idea")
    }
}
