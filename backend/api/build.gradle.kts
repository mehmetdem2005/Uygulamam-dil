plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ktor)
}

kotlin {
    jvmToolchain(17)
}

application {
    mainClass.set("com.mehmetdem.dil.backend.api.ApplicationKt")
}

dependencies {
    implementation(project(":backend:domain"))
    implementation(project(":backend:application"))
    implementation(project(":backend:infrastructure:deepseek"))
    implementation(project(":backend:infrastructure:source"))
    implementation(project(":backend:infrastructure:supabase"))
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.server.status.pages)
    implementation(libs.ktor.server.call.logging)
    implementation(libs.ktor.serialization.json)
    runtimeOnly(libs.logback.classic)
    testImplementation(kotlin("test-junit"))
    testImplementation(libs.junit)
}
