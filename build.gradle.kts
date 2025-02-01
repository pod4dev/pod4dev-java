plugins {
    id("idea")
    id("eclipse")
    id("java")
    id("maven-publish")
    id("signing")
    id("io.freefair.lombok") version "8.11"
}

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    implementation("io.github.pod4dev:libpod-java:5.3.2-1")
    implementation("com.squareup.okhttp3.sample:unixdomainsockets:3.14.9")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.slf4j:slf4j-simple:2.0.16")
    implementation(platform("org.junit:junit-bom:5.10.3"))
    implementation("org.junit.jupiter:junit-jupiter")
}

group = "io.github.pod4dev"

java {
    withJavadocJar()
    withSourcesJar()
}

tasks.test {
    useJUnitPlatform()
}
