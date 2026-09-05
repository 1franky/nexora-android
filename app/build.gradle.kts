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
        // 1.3.0: navegación real de los tiles del dashboard (patrimonio neto, disponible
        // -solo débito-, deuda, próximo pago y gastos del mes ya llevan a algún lado) y
        // nuevo tile "Quincena" (suma de pagos próximos de la quincena en curso).
        // 1.4.0: bloqueo de la app con huella digital (A10) con margen de gracia de 30s y
        // FLAG_SECURE, TokenStore cifrado en reposo (AES-256-GCM vía Android Keystore), y
        // recuperación de contraseña por OTP vía email (A11, backend B10 + web W9).
        // 1.5.0: integración con el SAT (A12, backend B11 + web W10) — conectar la e.firma
        // (protegido detrás del bloqueo con huella) y consultar/filtrar/descargar las
        // facturas (CFDI) que el backend sincroniza automáticamente del SAT.
        // 1.5.1: fix de un crash 100% reproducible al seleccionar el .cer/.key en Conexión
        // SAT (androidx.fragment desactualizado, arrastrado transitivamente por biometric,
        // incompatible con el rango de requestCode del selector de archivos moderno) y fix
        // de la sync SAT por rango de fechas (mandaba la fecha sin zona horaria).
        // 1.5.2: fix de un crash 100% reproducible al entrar a Notificaciones tras vincular
        // el SAT (NotificationType no tenía los valores SAT_SYNC_COMPLETED/SAT_SYNC_FAILED
        // que ya manda el backend desde B11) y fix visual en Movimientos: al ver todas las
        // cuentas, una transferencia resaltaba el nombre de la contraparte junto al monto en
        // vez de la cuenta dueña de ese monto, dando la impresión de signos invertidos.
        // 1.6.0: descarga y comparte la representación impresa (PDF) de una factura SAT
        // (A14, backend B13), junto al XML original que ya se podía descargar — el PDF
        // se genera del lado del backend a partir del mismo XML, no hace falta reconectar
        // la e.firma ni resincronizar nada.
        versionCode = 10
        versionName = "1.6.0"

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
    sourceSets["androidTest"].kotlin.srcDirs("src/androidTest/kotlin")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.biometric)
    // Fija fragment-ktx a una versión moderna: androidx.biometric:1.1.0 (2021)
    // arrastra transitivamente androidx.fragment:1.2.5, cuya
    // FragmentActivity.checkForValidRequestCode() exige requestCode < 0x10000.
    // El registro moderno de ActivityResultContracts (activity-compose 1.10.1)
    // genera requestCodes deliberadamente >= 0x10000 (rango reservado para no
    // chocar con los códigos "clásicos" de fragments/activities), así que con
    // la 1.2.5 vieja CUALQUIER launcher.launch() en una FragmentActivity revienta
    // con "Can only use lower 16 bits for requestCode" — es lo que pasaba al
    // seleccionar el .cer/.key en la pantalla de Conexión SAT. Gradle resuelve
    // por versión más alta entre todas las declaradas, así que esta dependencia
    // directa sustituye a la 1.2.5 transitiva sin tocar biometric.
    implementation(libs.androidx.fragment.ktx)
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
