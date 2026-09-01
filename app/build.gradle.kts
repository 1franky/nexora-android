import java.io.FileInputStream
import java.util.Properties

plugins {
    // Desde AGP 9.0, com.android.application ya trae el soporte de Kotlin
    // integrado — org.jetbrains.kotlin.android ya no se aplica aparte.
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

/**
 * Firma de release (A9): local (keystore.properties, gitignored — ver
 * keystore.properties.example) o CI (variables de entorno, ver
 * .github/workflows/release.yml). Sin ninguna de las dos, releaseSigning
 * queda con valores null y solo falla `assembleRelease`/`bundleRelease` —
 * el resto de tareas (debug incluido) no se ve afectado.
 */
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) load(FileInputStream(keystorePropertiesFile))
}

fun releaseSigningProperty(propertyKey: String, envVar: String): String? =
    System.getenv(envVar) ?: keystoreProperties.getProperty(propertyKey)

android {
    namespace = "com.nexora.android"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.nexora.android"
        minSdk = 26
        targetSdk = 37
        // 1.0.0: A1-A9 completos (plan.md, sección 9) — primer release firmado.
        // 1.1.0: edición de cuentas/tarjetas/compras/planes MSI-MCI, búsqueda en
        // Cuentas y Tarjetas, rediseño del dashboard (hero de patrimonio + accesos
        // rápidos funcionales) y conversión de moneda multi-currency (backlog notas.txt).
        // 1.2.0: editar/borrar movimientos, excluir AFORE/PPR al pagar tarjeta, fix de
        // navegación del bottom nav (volver al Dashboard desde el detalle de una tarjeta).
        versionCode = 4
        versionName = "1.2.0"

        // URL pública real de nexora-api (VPS). No hay sabor "local" todavía —
        // se agrega cuando haga falta apuntar a un backend en desarrollo.
        buildConfigField("String", "API_BASE_URL", "\"https://nexora-api.franciscolopez.uk/api/v1/\"")
    }

    signingConfigs {
        create("release") {
            val storeFilePath = releaseSigningProperty("storeFile", "KEYSTORE_PATH")
            if (storeFilePath != null) storeFile = file(storeFilePath)
            storePassword = releaseSigningProperty("storePassword", "KEYSTORE_PASSWORD")
            keyAlias = releaseSigningProperty("keyAlias", "KEY_ALIAS")
            keyPassword = releaseSigningProperty("keyPassword", "KEY_PASSWORD")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        jvmToolchain(17)
    }

    sourceSets["main"].kotlin.srcDirs("src/main/kotlin")
    sourceSets["test"].kotlin.srcDirs("src/test/kotlin")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.work.runtime.ktx)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    debugImplementation(libs.androidx.ui.tooling)

    implementation(libs.retrofit.core)
    implementation(libs.retrofit.converter.kotlinx.serialization)
    implementation(libs.okhttp.core)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.test.manifest)
}
