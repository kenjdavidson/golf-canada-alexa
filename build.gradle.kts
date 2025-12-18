
plugins {
    kotlin("jvm") version "1.9.23"

    id("org.openapi.generator") version("7.8.0")
}

group = "kjd.golfcanada"
version = "1.1-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
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
    implementation("org.openapitools:openapi-generator-gradle-plugin:6.6.0")
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
    
    // Integration test source set
    create("integrationTest") {
        kotlin {
            compileClasspath += sourceSets["main"].output
            runtimeClasspath += sourceSets["main"].output
        }
    }
}

// Configure integrationTest dependencies
val integrationTestImplementation by configurations.getting {
    extendsFrom(configurations["testImplementation"])
}

val integrationTestRuntimeOnly by configurations.getting {
    extendsFrom(configurations["testRuntimeOnly"])
}

dependencies {
    integrationTestImplementation("io.kotest:kotest-runner-junit5:5.7.2")
    integrationTestImplementation("org.slf4j:slf4j-simple:2.0.9")
}

tasks.test {
    useJUnitPlatform()
}

// Integration test task
val integrationTest = task<Test>("integrationTest") {
    description = "Runs integration tests."
    group = "verification"
    
    testClassesDirs = sourceSets["integrationTest"].output.classesDirs
    classpath = sourceSets["integrationTest"].runtimeClasspath
    
    useJUnitPlatform()
    
    shouldRunAfter(tasks.test)
    
    // Set MOCK_API environment variable for integration tests
    // Note: Currently defaults to "true" but the mock implementation is not complete
    // See MOCKING_FRAMEWORK.md for details on current limitations
    environment("MOCK_API", project.findProperty("MOCK_API")?.toString() ?: "true")
    environment("SKILL_ID", "test-skill-id")
    environment("CLIENT_ID", "test-client-id")
    environment("CLIENT_SECRET", "test-client-secret")
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

task("packageJar", Zip::class) {
    group = "release"

    into("lib") {
        from(tasks.jar)
        from(configurations.runtimeClasspath)
    }

    dependsOn(tasks.test)
}

tasks.compileKotlin {
    dependsOn("openApiGenerate")
}