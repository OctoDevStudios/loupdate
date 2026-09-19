plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.loupdate.example"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.loupdate.example"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        // Override in local secrets.properties (gitignored) — see secrets.properties.example
        val secrets = rootProject.file("examples/kotlin-app/secrets.properties")
        val props = mutableMapOf(
            "LOUPDATE_ENDPOINT" to "https://s3.eu-central-003.backblazeb2.com",
            "LOUPDATE_REGION" to "eu-central-003",
            "LOUPDATE_BUCKET" to "Loupdate",
            "LOUPDATE_KEY_ID" to "",
            "LOUPDATE_KEY_SECRET" to "",
            "LOUPDATE_APP_ID" to "demo",
        )
        if (secrets.exists()) {
            secrets.readLines()
                .filter { it.contains("=") && !it.trimStart().startsWith("#") }
                .forEach {
                    val (k, v) = it.split("=", limit = 2)
                    props[k.trim()] = v.trim()
                }
        }
        props.forEach { (k, v) ->
            buildConfigField("String", k, "\"${v.replace("\"", "\\\"")}\"")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation(project(":loupdate-core"))
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.0")
}
