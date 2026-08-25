plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

val releaseKeystorePath = providers.environmentVariable("MEALOS_KEYSTORE_PATH").orNull
val releaseKeystorePassword = providers.environmentVariable("MEALOS_KEYSTORE_PASSWORD").orNull
val releaseKeyAlias = providers.environmentVariable("MEALOS_KEY_ALIAS").orNull
val releaseKeyPassword = providers.environmentVariable("MEALOS_KEY_PASSWORD").orNull
val recipeBridgeUrl = providers.environmentVariable("MEALOS_BRIDGE_URL").orNull.orEmpty()
val recipeBridgeToken = providers.environmentVariable("MEALOS_BRIDGE_TOKEN").orNull.orEmpty()
val releaseSigningReady = listOf(
    releaseKeystorePath,
    releaseKeystorePassword,
    releaseKeyAlias,
    releaseKeyPassword
).all { !it.isNullOrBlank() }

android {
    namespace = "com.mealos"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.mealos"
        minSdk = 26
        targetSdk = 37
        versionCode = providers.environmentVariable("MEALOS_VERSION_CODE").orNull?.toIntOrNull() ?: 4_000
        versionName = providers.environmentVariable("MEALOS_VERSION_NAME").orNull ?: "0.4.0"
        buildConfigField("String", "RECIPE_BRIDGE_URL", "\"${recipeBridgeUrl.replace("\\", "\\\\").replace("\"", "\\\"")}\"")
        buildConfigField("String", "RECIPE_BRIDGE_TOKEN", "\"${recipeBridgeToken.replace("\\", "\\\\").replace("\"", "\\\"")}\"")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    signingConfigs {
        if (releaseSigningReady) {
            create("release") {
                storeFile = file(releaseKeystorePath!!)
                storePassword = releaseKeystorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        getByName("release") {
            if (releaseSigningReady) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging.resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.08.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.core:core-ktx:1.19.0")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.11.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.11.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.11.0")
    implementation("androidx.datastore:datastore-preferences:1.2.1")
    implementation("androidx.window:window:1.5.1")
    implementation("com.google.android.gms:play-services-mlkit-document-scanner:16.0.0")
    implementation("com.google.mlkit:text-recognition:16.0.1")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
}
