plugins {
    java
    id("com.gradleup.shadow") version "8.3.5"
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:${providers.gradleProperty("paperApiVersion").get()}")

    implementation(project(":api"))
    implementation(project(":core"))
    implementation(project(":adapter-common"))
    // TODO: paperweight-userdev модули не всегда отдают reobf-jar как выход обычной
    // project()-зависимости "из коробки". Если shadowJar соберёт adapter-1_21_4
    // с Mojang-именами вместо переотображённых — заменить на явную зависимость
    // от артефакта задачи reobfJar модуля :adapter-1_21_4.
    implementation(project(":adapter-1_21_4"))
}

tasks.shadowJar {
    archiveClassifier.set("")
    archiveBaseName.set("chunk-regen-lib")

    // Это канонический дистрибутив плагина-провайдера: классы НЕ релоцируются,
    // чтобы Bukkit.getServicesManager().load(ChunkRegenerator.class) видел один
    // и тот же класс у всех плагинов-потребителей. Relocation нужен только тем,
    // кто встраивает библиотеку приватно в собственный shadow-конфиг — см. README.md.
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

tasks.processResources {
    filesMatching("plugin.yml") {
        expand("version" to project.version)
    }
}
