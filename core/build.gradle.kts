plugins {
    `java-library`
    `maven-publish`
}

java {
    withJavadocJar()
}

dependencies {
    api(project(":api"))
    compileOnly("io.papermc.paper:paper-api:${providers.gradleProperty("paperApiVersion").get()}")

    testImplementation("io.papermc.paper:paper-api:${providers.gradleProperty("paperApiVersion").get()}")
    testImplementation(platform("org.junit:junit-bom:${providers.gradleProperty("junitBomVersion").get()}"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.mockito:mockito-core:${providers.gradleProperty("mockitoVersion").get()}")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            artifactId = "chunkregenlib-core"
            from(components["java"])
        }
    }
}

tasks.jar {
    archiveBaseName.set("chunkregenlib-core")
}
