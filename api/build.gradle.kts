plugins {
    `java-library`
    `maven-publish`
}

java {
    // Публикует artifactId-version-javadoc.jar вместе с обычным jar'ом —
    // нужно, чтобы Reposilite мог отрендерить javadoc (см. /javadoc/<repo>/<gav>).
    withJavadocJar()
}

dependencies {
    // Только публичный Paper API — никакого paperweight/NMS в этом модуле.
    compileOnly("io.papermc.paper:paper-api:${providers.gradleProperty("paperApiVersion").get()}")

    testImplementation(platform("org.junit:junit-bom:${providers.gradleProperty("junitBomVersion").get()}"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            artifactId = "chunkregenlib-api"
            from(components["java"])
        }
    }
}

tasks.jar {
    archiveBaseName.set("chunkregenlib-api")
}
