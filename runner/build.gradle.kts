plugins {
    java
    application
    id("org.springframework.boot") version "4.0.3"
	id("io.spring.dependency-management") version "1.1.7"
}

group = "se.lnu"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.fasterxml.jackson.core:jackson-databind:2.18.3")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

application {
    mainClass.set("se.lnu.Main")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
