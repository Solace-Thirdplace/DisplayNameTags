import org.gradle.api.GradleException

pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://repo.papermc.io/repository/maven-public/")
    }
}

plugins {
    id("com.gradle.enterprise") version("3.15")
}

gradleEnterprise {
    if (System.getenv("CI") != null) {
        buildScan {
            publishAlways()
            termsOfServiceUrl = "https://gradle.com/terms-of-service"
            termsOfServiceAgree = "yes"
        }
    }
}

gradle.settingsEvaluated {
    val current = org.gradle.api.JavaVersion.current()
    if (current != org.gradle.api.JavaVersion.VERSION_21) {
        throw GradleException(
            "This project requires Java 21 to run Gradle. Current JVM: " +
                "${System.getProperty("java.version")} at ${System.getProperty("java.home")}. " +
                "Set JAVA_HOME (or JDK21_HOME) to a JDK 21 installation."
        )
    }
}