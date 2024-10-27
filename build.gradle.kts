plugins {
    kotlin("jvm") version "1.9.23"
}

group = "kjd.golfcanada"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))

    implementation("com.amazon.alexa:ask-sdk-core:2.86.0")
    implementation("com.amazon.alexa:ask-sdk-lambda-support:2.86.0")
    implementation("com.amazon.alexa:ask-sdk-servlet-support:2.86.0")
    implementation("com.amazon.alexa:ask-sdk-apache-client:2.86.0")
    implementation("com.amazon.alexa:alexa-skills-kit:1.1.2")
    implementation("com.amazonaws:aws-lambda-java-core:1.0.0")

    implementation("org.apache.logging.log4j:log4j-core:2.24.1")
    implementation("org.apache.commons:commons-lang3:3.3.2")
    implementation("org.slf4j:slf4j-api:1.7.10")
}

tasks.test {
    useJUnitPlatform()
}