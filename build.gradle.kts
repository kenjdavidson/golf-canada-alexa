
plugins {
    kotlin("jvm") version "1.9.23"

    id("org.openapi.generator") version("7.8.0")
}

group = "kjd.golfcanada"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    testImplementation("io.kotest:kotest-runner-junit5:5.7.2")
    testImplementation("io.mockk:mockk:1.13.13")

    implementation("com.amazon.alexa:ask-sdk-core:2.86.0")
    implementation("com.amazon.alexa:ask-sdk-lambda-support:2.86.0")
    implementation("com.amazon.alexa:ask-sdk-servlet-support:2.86.0")
    implementation("com.amazon.alexa:ask-sdk-apache-client:2.86.0")
    implementation("com.amazon.alexa:alexa-skills-kit:1.1.2")
    implementation("com.amazonaws:aws-lambda-java-core:1.2.3")
    implementation("com.amazonaws:aws-lambda-java-events:3.14.0")

    implementation("org.apache.logging.log4j:log4j-core:2.24.1")
    implementation("org.apache.commons:commons-lang3:3.3.2")
    implementation("org.slf4j:slf4j-api:1.7.10")

    implementation("org.openapitools:openapi-generator-gradle-plugin:6.6.0")
    implementation("com.squareup.okhttp3:okhttp:4.11.0")
    implementation("com.squareup.moshi:moshi:1.15.0")
    implementation("com.squareup.moshi:moshi-kotlin:1.15.0")
}

sourceSets {
    main {
        kotlin {
            srcDir("${buildDir}/generated/src/main/kotlin")
        }
    }
}

tasks.test {
    useJUnitPlatform()
}

openApiGenerate {
    generatorName = "kotlin"
    inputSpec = "$rootDir/src/main/resources/client/golfcanada.yaml"
    outputDir = "${buildDir}/generated"
    apiPackage = "kjd.golfcanada.client.api"
    invokerPackage = "kjd.golfcanada.client.invoker"
    modelPackage = "kjd.golfcanada.client.model"
}

