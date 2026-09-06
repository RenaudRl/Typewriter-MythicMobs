plugins {
    kotlin("jvm") version "2.2.10"
    id("com.typewritermc.module-plugin") version "2.2.0"
}

repositories {
    maven("https://jitpack.io/")
    mavenCentral()
    maven("https://maven.typewritermc.com/beta/")
    maven("https://maven.typewritermc.com/external/")
    maven("https://mvn.lumine.io/repository/maven-public/")
    maven("https://repo.codemc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.lumine:Mythic-Dist:5.11.2")
    compileOnly("com.typewritermc:QuestExtension:0.9.0")
    compileOnly("com.typewritermc:BasicExtension:0.9.0")
}

group = "btcrenaud"
version = "0.0.9"

typewriter {
    namespace = "btcrenaud"
    extension {
        name = "MythicMobs"
        shortDescription = "MythicMobs extension for Typewriter"
        description = "A comprehensive TypeWriter extension providing advanced gameplay features for Minecraft servers on Paper 1.21+. Fully compatible with the official TypeWriter engine and PlaceholderAPI."
        engineVersion = "0.9.0-beta-176"
        channel = com.typewritermc.moduleplugin.ReleaseChannel.BETA
        paper()
        dependencies {
            dependency("typewritermc", "Quest")
        }
    }
}

kotlin {
    jvmToolchain(21)
}
