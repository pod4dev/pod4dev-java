plugins {
    id("idea")
    id("eclipse")
    id("java")
    id("maven-publish")
    id("signing")
    id("org.jreleaser") version "1.15.0"
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
version = "0.3.2"

java {
    withJavadocJar()
    withSourcesJar()
}

tasks.test {
    useJUnitPlatform()
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            groupId = project.group.toString()
            artifactId = project.name
            version = project.version.toString()

            from(components["java"])
            pom {
                packaging = "jar"
                name.set("pod4dev-java")
                url.set("https://github.com/pod4dev/pod4dev-java")
                description.set("Podman Containers for Java")

                scm {
                    url.set("https://github.com/pod4dev/pod4dev-java")
                    connection.set("scm:git:git://github.com/pod4dev/pod4dev-java.git")
                    developerConnection.set("scm:git:git://github.com/pod4dev/pod4dev-java.git")
                }

                inceptionYear = "2025"

                developers {
                    developer {
                        name.set("Pod4Dev Team")
                    }
                }

                licenses {
                    license {
                        name.set("MIT")
                        url.set("https://opensource.org/license/mit")
                    }
                }
            }
        }
    }
    repositories {
        maven {
            setUrl(layout.buildDirectory.dir("staging-deploy"))
        }
    }
}

jreleaser {
    project {
        inceptionYear = "2025"
        author("Pod4Dev Team")
        description = "Podman Containers for Java"
        license = "MIT"
    }
    gitRootSearch = true
    release {
        github {
            // https://github.com/jreleaser/jreleaser/discussions/367
            token = "dummy"
        }
    }
    signing {
        setActive("ALWAYS")
        armored = true
        passphrase = properties["signing.gnupg.passphrase"].toString()
        setMode("COMMAND")
        command {
            executable = properties["signing.gnupg.executable"].toString()
            keyName = properties["signing.gnupg.keyName"].toString()
        }
    }
    deploy {
        maven {
            mavenCentral {
                create("sonatype") {
                    setActive("ALWAYS")
                    url = "https://central.sonatype.com/api/v1/publisher"
                    stagingRepository("build/staging-deploy")
                    username.set(properties["ossrhUsername"].toString())
                    password.set(properties["ossrhPassword"].toString())
                }
            }
        }
    }
}
