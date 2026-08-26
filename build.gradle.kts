import org.gradle.api.publish.PublishingExtension
import org.gradle.jvm.toolchain.JavaLanguageVersion

allprojects {
    group = providers.gradleProperty("libraryGroup").get()
    version = providers.gradleProperty("libraryVersion").get()

    repositories {
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/")
    }
}

// Каждый модуль сам выбирает нужный плагин (java-library для api/core/adapter-common,
// java + paperweight-userdev для adapter-1_21_4, java + shadow для plugin-bootstrap).
// Здесь централизованы только toolchain/release, применяемые ко всем, кто применил "java".
subprojects {
    plugins.withId("java") {
        configure<JavaPluginExtension> {
            toolchain {
                // Разработка ведётся на Java 25, но байткод таргетится на Java 21
                // (минимум для Paper 1.20.5+ / Leaf 1.21.x).
                languageVersion.set(JavaLanguageVersion.of(25))
            }
        }

        tasks.withType<JavaCompile>().configureEach {
            options.encoding = "UTF-8"
            options.release.set(21)
        }

        tasks.withType<Javadoc>().configureEach {
            (options as StandardJavadocDocletOptions).encoding = "UTF-8"
        }
    }

    // Публичный релизный репозиторий. Логин/пароль берутся из ~/.gradle/gradle.properties
    // (seetchRepoUser/seetchRepoPassword) — никогда не хранить их в файлах проекта.
    plugins.withId("maven-publish") {
        configure<PublishingExtension> {
            repositories {
                maven {
                    name = "seetchRepoReleases"
                    url = uri("https://repo.seetch.ru/releases")
                    credentials {
                        username = providers.gradleProperty("seetchRepoUser").orNull
                        password = providers.gradleProperty("seetchRepoPassword").orNull
                    }
                }
            }
        }
    }
}
