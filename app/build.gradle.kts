import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.finanzasapp"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.finanzasapp"
        minSdk = 26
        targetSdk = 36
        versionCode = 9
        versionName = "1.0.0"

        // Solo procesadores ARM (todos los teléfonos Android actuales). Las
        // librerías nativas de SQLCipher para x86 son peso muerto en un teléfono.
        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a")
        }
    }

    // La llave de release vive FUERA del repositorio. La ruta del archivo de
    // propiedades sale de FINANZAS_KEYSTORE o de ~/.finanzasapp/keystore.properties.
    // Sin ella, la release se compila sin firmar (como antes).
    val keystoreFile = System.getenv("FINANZAS_KEYSTORE")?.let(::file)
        ?: file("${System.getProperty("user.home")}/.finanzasapp/keystore.properties")
    val releaseSigning = if (keystoreFile.exists()) {
        val props = Properties().apply { keystoreFile.inputStream().use(::load) }
        signingConfigs.create("release") {
            storeFile = file(props.getProperty("storeFile"))
            storePassword = props.getProperty("storePassword")
            keyAlias = props.getProperty("keyAlias")
            keyPassword = props.getProperty("keyPassword")
        }
    } else {
        null
    }

    buildToolsVersion = "36.0.0"

    buildTypes {
        release {
            // R8 quita código y recursos sin usar (los iconos que no se usan,
            // partes de PDFBox...) y optimiza: la app pesa menos y va más fluida.
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = releaseSigning
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    // Mapas de caracteres chinos, japoneses y coreanos de PDFBox (están en
    // assets): los extractos que se importan están en español.
    androidResources {
        ignoreAssetsPatterns += listOf(
            "UniCNS*", "UniGB*", "UniJIS*", "UniKS*",
            "Adobe-CNS1*", "Adobe-GB1*", "Adobe-Japan1*", "Adobe-Korea1*"
        )
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            // Tablas de criptografía postcuántica de BouncyCastle (dependencia de
            // PDFBox para PDF cifrados): ~4 MB que esta app nunca usa.
            excludes += "org/bouncycastle/pqc/**"

        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    // Catálogo de iconos para que cada categoría tenga el suyo. En release,
    // R8 elimina los que no se usan.
    implementation(libs.androidx.compose.material.icons.extended)
    // Recordatorios locales de pagos programados (fase 5).
    implementation(libs.androidx.work.runtime.ktx)

    implementation(libs.androidx.navigation.compose)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Cifrado real en reposo: SQLCipher + la interfaz de androidx.sqlite
    // que usan Room y el driver cifrado.
    implementation(libs.androidx.sqlite)
    implementation(libs.sqlcipher.android)

    // Lectura de PDF en el propio teléfono. No usa red ni servicios externos.
    implementation(libs.pdfbox.android)

    implementation(libs.androidx.datastore.preferences)

    implementation(libs.kotlinx.coroutines.android)

    debugImplementation(libs.androidx.compose.ui.tooling)
}
