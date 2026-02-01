plugins {
    kotlin("jvm") version "2.2.10"
    id("com.typewritermc.module-plugin")
}

group = "com.typewritermc.mythicmobsextension"
version = "0.9.0"

repositories {
    mavenCentral()
    maven("https://mvn.lumine.io/repository/maven-public/")
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://maven.typewritermc.com/external")
}

dependencies {
    implementation("com.typewritermc:BasicExtension:0.9.0")
    implementation("com.typewritermc:EntityExtension:0.9.0")
    compileOnly("io.lumine:Mythic-Dist:5.8.2")
    compileOnly("com.github.retrooper:packetevents-spigot:2.11.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
}

typewriter {
    namespace = "typewritermc"

    extension {
        name = "MythicMobs"
        shortDescription = "Integrate MythicMobs with Typewriter."
        description = """
            |The MythicMobs Extension allows you to create MyticMobs, and trigger Skills from Typewriter.
            |Create cool particles during cinematics or have dialgues triggered when interacting with a MythicMob.
        """.trimMargin()
        engineVersion = file("../../version.txt").readText().trim()
        channel = com.typewritermc.moduleplugin.ReleaseChannel.BETA
        dependencies {
            dependency("typewritermc", "Basic")
            dependency("typewritermc", "Entity")
        }

        paper {
            dependency("MythicMobs")
            dependency("packetevents")
        }
    }
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}
