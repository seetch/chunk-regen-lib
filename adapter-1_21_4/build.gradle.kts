plugins {
    java
    `maven-publish`
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.19"
}

dependencies {
    paperweight.paperDevBundle(providers.gradleProperty("paperweightDevBundleVersion").get())

    implementation(project(":api"))
    implementation(project(":core"))
    implementation(project(":adapter-common"))
}

// paperweight-userdev компилирует и тестирует модуль против Mojang-маппингов;
// на выходе assemble должен собирать reobf-jar (переотображённый в Spigot-маппинги
// для рантайма), а не "сырой" jar с Mojang-именами.
tasks.assemble {
    dependsOn(tasks.named("reobfJar"))
}

// Публикуем reobf-jar (переотображённый в рантайм-маппинги), а не сырой jar
// components["java"] — тот собран против Mojang-маппингов и на реальном сервере не заработает.
publishing {
    publications {
        create<MavenPublication>("maven") {
            artifactId = "chunkregenlib-adapter-1_21_4"
            artifact(tasks.named("reobfJar"))
        }
    }
}
