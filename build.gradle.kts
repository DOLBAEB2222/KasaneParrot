/*
 * KasaneParrot — почтовая служба Kasane-попугаев (Paper 1.21.8)
 *
 * Сборка:  ./gradlew build            → build/libs/KasaneParrot-<version>.jar
 * Запуск тестового сервера появится на этапе 3 (paperweight runServer).
 *
 * Требуется JDK 21+ (java toolchain не фиксируем, чтобы собиралось и на 25:
 * совместимость с 1.21.8 задаётся через --release 21).
 */

plugins {
    `java-library`
}

group = "dev.dolbaeb"
version = "1.0.0-SNAPSHOT"
description = "KasaneParrot — почтовая служба Kasane-попугаев для Paper 1.21.8"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/") {
        name = "papermc"
    }
}

dependencies {
    // Paper API только для компиляции: всё нужное (adventure, guava) уже в сервере.
    compileOnly("io.papermc.paper:paper-api:1.21.8-R0.1-SNAPSHOT")
}

java {
    // Не фиксируем toolchain: локально может стоять JDK 21..25.
    // Корректность байткода гарантирует options.release ниже.
    withSourcesJar()
}

tasks.processResources {
    val props = mapOf("version" to project.version.toString())
    inputs.properties(props)
    filesMatching("plugin.yml") {
        expand(props)
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release = 21
}

tasks.withType<Javadoc>().configureEach {
    options.encoding = "UTF-8"
}
