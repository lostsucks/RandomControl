plugins {
    id("java-library")
    id("xyz.jpenilla.run-paper") version "1.0.6"
    id("net.minecrell.plugin-yml.bukkit") version "0.5.2"
}

group = "one.eim"
version = "1.1.1"
description = "Liberate your server from the RNG loving bourgeoisie! Paper plugin to enable RNG manipulation."

repositories {
    maven("https://repo.papermc.io/repository/maven-public")
}

dependencies {
    compileOnly("io.papermc.paper", "paper-api", "1.19.2-R0.1-SNAPSHOT")
}

tasks {

    compileJava {
        options.encoding = Charsets.UTF_8.name()

        doFirst {
            options.compilerArgs.addAll(listOf("--release", "8"))
        }
    }

    runServer {
        minecraftVersion("1.19.2")
        systemProperty("Paper.IgnoreJavaVersion", "true")
    }
}

bukkit {
    website = "https://github.com/e-im/RandomCOntrol"
    authors = listOf("e-im")
    main = "one.eim.randomcontrol.RandomControl"
    apiVersion = "1.13"
}
