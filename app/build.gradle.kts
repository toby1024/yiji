import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

val localProperties = Properties().apply {
    
    val localFile = rootProject.file("local.properties")
    if (localFile.exists()) {
        localFile.inputStream().use(::load)
    }
}

val requestedTasks = gradle.startParameter.taskNames.joinToString(" ").lowercase()
val isReleaseBuildRequested = requestedTasks.contains("release")

fun readConfig(key: String): String? {
    val fromLocal = localProperties.getProperty(key)?.trim().orEmpty()
    if (fromLocal.isNotEmpty()) return fromLocal

    val fromGradleProperty = providers.gradleProperty(key).orNull?.trim().orEmpty()
    if (fromGradleProperty.isNotEmpty()) return fromGradleProperty

    val fromEnv = System.getenv(key)?.trim().orEmpty()
    if (fromEnv.isNotEmpty()) return fromEnv

    return null
}

fun readConfigWithFallback(key: String, fallback: String): String {
    return readConfig(key) ?: fallback
}

fun requireReleaseConfig(key: String): String {
    return readConfig(key) ?: error(
        "Missing required config '$key' for release build. " +
            "Provide it via local.properties, gradle.properties, or environment variable."
    )
}

fun maskSensitive(value: String): String {
    if (value.isBlank()) return "<empty>"
    return when {
        value.length <= 8 -> "****"
        else -> value.take(4) + "****" + value.takeLast(4)
    }
}

val apiBaseUrl = if (isReleaseBuildRequested) {
    requireReleaseConfig("API_BASE_URL")
} else {
    readConfigWithFallback("API_BASE_URL", "https://example.com/")
}
val apiKey = if (isReleaseBuildRequested) {
    requireReleaseConfig("API_KEY")
} else {
    readConfigWithFallback("API_KEY", "demo-key")
}
val apiSecret = if (isReleaseBuildRequested) {
    requireReleaseConfig("API_SECRET")
} else {
    readConfigWithFallback("API_SECRET", "demo-secret")
}
val googleWebClientId = if (isReleaseBuildRequested) {
    requireReleaseConfig("GOOGLE_WEB_CLIENT_ID")
} else {
    readConfigWithFallback("GOOGLE_WEB_CLIENT_ID", "")
}

gradle.taskGraph.whenReady {
    val hasAppBuildTask = allTasks.any { it.path.startsWith(":app:") }
    if (!hasAppBuildTask) return@whenReady

    logger.lifecycle("[BuildConfig] isReleaseBuildRequested=$isReleaseBuildRequested")
    logger.lifecycle("[BuildConfig] API_BASE_URL=$apiBaseUrl")
    logger.lifecycle("[BuildConfig] API_KEY=${maskSensitive(apiKey)}")
    logger.lifecycle("[BuildConfig] API_SECRET=${maskSensitive(apiSecret)}")
    logger.lifecycle("[BuildConfig] GOOGLE_WEB_CLIENT_ID=${maskSensitive(googleWebClientId)}")
}

android {
    namespace = "com.bluearcyiji"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.bluearcyiji"
        minSdk = 24
        targetSdk = 36
        versionCode = 13
        versionName = "1.0"

        buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")
        buildConfigField("String", "API_KEY", "\"$apiKey\"")
        buildConfigField("String", "API_SECRET", "\"$apiSecret\"")
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"$googleWebClientId\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.billing)
    implementation(libs.androidx.material.icons)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}