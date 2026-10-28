import org.jetbrains.kotlin.gradle.dsl.JvmTarget
plugins { alias(libs.plugins.androidApplication); alias(libs.plugins.composeCompiler) }
kotlin { compilerOptions { jvmTarget = JvmTarget.JVM_17 } }
android {
    namespace = "pe.upeu.ecorecicla"
    compileSdk = 36
    defaultConfig { applicationId = "pe.upeu.ecorecicla"; minSdk = 24; targetSdk = 36; versionCode = 1; versionName = "1.0" }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    buildFeatures { compose = true }
    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
}
dependencies {
    implementation(project(":shared"))
    implementation(libs.androidx.activity.compose)
    implementation(libs.koin.android)
    implementation(libs.ktor.client.cio)
    implementation("app.cash.sqldelight:android-driver:2.1.0")
}
