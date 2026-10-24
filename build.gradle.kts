
plugins {
    kotlin("jvm") version "1.9.23"
    kotlin("kapt") version "1.9.23"
    id("io.micronaut.application") version "4.4.2"
    id("io.micronaut.aot") version "4.4.2"
    id("org.openapi.generator") version("7.8.0")
}

group = "kjd.golfcanada"
version = "1.1-SNAPSHOT"

repositories {
    mavenCentral()
}

micronaut {
    version("4.6.3")
    runtime("lambda_java")
    testRuntime("junit5")
    processing {
        incremental(true)
        annotations("kjd.golfcanada.*")
    }
    aot {
        optimizeServiceLoading.set(false)
        convertYamlToJava.set(false)
        precomputeOperations.set(true)
        cacheEnvironment.set(true)
        optimizeClassLoading.set(true)
        deduceEnvironment.set(true)
    }
}

dependencies {
    kapt("io.micronaut:micronaut-inject-java")
    kapt("io.micronaut.serde:micronaut-serde-processor")

    implementation("io.micronaut.aws:micronaut-function-aws")
    implementation("io.micronaut.aws:micronaut-function-aws-alexa")
    implementation("io.micronaut.serde:micronaut-serde-jackson")
    implementation("io.micronaut.kotlin:micronaut-kotlin-runtime")

    testImplementation(kotlin("test"))
    testImplementation("io.kotest:kotest-runner-junit5:5.7.2")
    testImplementation("io.mockk:mockk:1.13.13")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")

    implementation("com.amazon.alexa:ask-sdk:2.86.0")
    implementation("com.amazon.alexa:ask-sdk-apache-client:2.86.0")
    implementation("com.amazon.alexa:alexa-skills-kit:1.1.2")
    implementation("com.amazonaws:aws-lambda-java-core:1.2.3")
    implementation("com.amazonaws:aws-lambda-java-events:3.14.0")

    implementation("org.apache.logging.log4j:log4j-core:2.20.0")
    implementation("org.apache.logging.log4j:log4j-api:2.20.0")
    implementation("org.apache.logging.log4j:log4j-slf4j-impl:2.20.0")
    implementation("com.amazonaws:aws-lambda-java-log4j2:1.6.0")

    implementation("org.apache.commons:commons-lang3:3.3.2")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.moshi:moshi:1.15.0")
    implementation("com.squareup.moshi:moshi-kotlin:1.15.0")
}

sourceSets {
    main {
        kotlin {
            srcDir("${buildDir}/generated/aws")
            srcDir("${buildDir}/generated/openapi/src/main/kotlin")
        }
    }
}

tasks.test {
    useJUnitPlatform()
}

task("bundleOpenApiSpec", Exec::class) {
    group = "openapi"
    description = "Bundle modular OpenAPI spec into a single file"

    commandLine("npx", "@apidevtools/swagger-cli@4.0.4", "bundle",
        "$rootDir/src/main/resources/client/golfcanada.yaml",
        "-o", "${buildDir}/openapi/golfcanada-bundled.yaml",
        "-t", "yaml")

    inputs.dir("$rootDir/src/main/resources/client")
    outputs.file("${buildDir}/openapi/golfcanada-bundled.yaml")

    doFirst {
        mkdir("${buildDir}/openapi")
    }
}

openApiGenerate {
    generatorName = "kotlin"
    inputSpec = "${buildDir}/openapi/golfcanada-bundled.yaml"
    outputDir = "${buildDir}/generated/openapi"
    apiPackage = "kjd.golfcanada.client.api"
    invokerPackage = "kjd.golfcanada.client.invoker"
    modelPackage = "kjd.golfcanada.client.model"
    typeMappings = mapOf(
        "identifier" to "kotlin.Long"
    )
}

tasks.named("openApiGenerate") {
    dependsOn("bundleOpenApiSpec")
}

tasks.compileKotlin {
    dependsOn("openApiGenerate")
}