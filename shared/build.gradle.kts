import org.jetbrains.kotlin.gradle.dsl.JvmTarget
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
    id("app.cash.sqldelight")
}
kotlin {
    android {
        namespace = "pe.upeu.ecorecicla.shared"
        compileSdk = 36
        minSdk = 24
        compilerOptions { jvmTarget = JvmTarget.JVM_17 }
        androidResources { enable = true }
        withHostTest { isIncludeAndroidResources = true }
    }
    jvm()
    sourceSets {
        commonMain.dependencies {
            api(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.kotlinx.serialization.json)
            implementation("app.cash.sqldelight:runtime:2.1.0")
            implementation("app.cash.sqldelight:coroutines-extensions:2.1.0")
            implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.2")
        }
        androidMain.dependencies {
            implementation(libs.ktor.client.cio)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.ktor.client.mock)
        }
        jvmTest.dependencies { implementation("app.cash.sqldelight:sqlite-driver:2.1.0") }
        getByName("androidHostTest").dependencies { implementation("app.cash.sqldelight:sqlite-driver:2.1.0") }
    }
}
sqldelight { databases { create("EcoDatabase") { packageName.set("pe.upeu.ecorecicla.db") } } }
