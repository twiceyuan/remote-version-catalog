import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.5.21"
    id("java-gradle-plugin")
    id("maven-publish")
}

group = "io.github.twiceyuan"
version = "1.1"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    implementation(gradleApi())
}

gradlePlugin {
    plugins {
        create("RemoteVersionCatalog") {
            id = "remote-version-catalog"
            implementationClass = "io.github.twiceyuan.rvc.RemoteVersionCatalogPlugin"
        }
    }
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}
