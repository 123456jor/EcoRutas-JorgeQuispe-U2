import org.jetbrains.kotlin.gradle.dsl.JvmTarget
plugins { alias(libs.plugins.androidApplication); alias(libs.plugins.composeCompiler) }
kotlin { compilerOptions { jvmTarget = JvmTarget.JVM_17 } }
android {
    namespace = "pe.upeu.ecorecicla"
    compileSdk = 36
    defaultConfig { applicationId = "pe.upeu.ecorecicla"; minSdk = 24; targetSdk = 36; versionCode = 1; versionName = "1.0" }
    compileOptions { isCoreLibraryDesugaringEnabled = true; sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    buildFeatures { compose = true; buildConfig = true }
    buildTypes {
        debug { buildConfigField("String","API_BASE_URL","\"http://10.0.2.2:8080\"") }
        release { buildConfigField("String","API_BASE_URL","\""+(providers.gradleProperty("eco.apiUrl").orNull ?: "")+"\"") }
    }
    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
}
dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")
    implementation(project(":shared"))
    implementation(libs.androidx.activity.compose)
    implementation(libs.koin.android)
    implementation(libs.ktor.client.cio)
    implementation("app.cash.sqldelight:android-driver:2.1.0")
}
