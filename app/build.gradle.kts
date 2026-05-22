import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

// Read MAPS_API_KEY from local.properties (gitignored). Empty string is fine for builds —
// the map screens just won't render tiles without a real key.
val mapsApiKey: String = run {
    val props = Properties()
    val file = rootProject.file("local.properties")
    if (file.exists()) props.load(file.inputStream())
    props.getProperty("MAPS_API_KEY", "")
}

// Read release-signing config from keystore.properties (gitignored). If the file isn't there,
// release builds fall through to whatever default Gradle wants to do (typically: unsigned, or
// signed with the debug keystore depending on AGP version). Personal-install workflow:
//   1. `keytool -genkey -v -keystore release.keystore -alias dangodiary -keyalg RSA -keysize 2048 -validity 36500`
//   2. Drop the resulting `release.keystore` in the repo root.
//   3. Create `keystore.properties` next to it with:
//        storeFile=release.keystore
//        storePassword=<your password>
//        keyAlias=dangodiary
//        keyPassword=<your password>
//   4. Back up the keystore file + password somewhere safe. Losing either breaks the ability
//      to ever update the installed app.
val keystoreProps: Properties? = run {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) Properties().apply { load(file.inputStream()) } else null
}

android {
    namespace = "com.dangodiary"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.dangodiary"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0"

        manifestPlaceholders["MAPS_API_KEY"] = mapsApiKey
        buildConfigField("String", "MAPS_API_KEY", "\"$mapsApiKey\"")
    }

    signingConfigs {
        keystoreProps?.let { props ->
            create("release") {
                storeFile = rootProject.file(props.getProperty("storeFile"))
                storePassword = props.getProperty("storePassword")
                keyAlias = props.getProperty("keyAlias")
                keyPassword = props.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            // Only attach the release signing config when keystore.properties is present.
            // Lets fresh clones run `./gradlew assembleDebug` without setup; only
            // `assembleRelease` needs the keystore.
            keystoreProps?.let { signingConfig = signingConfigs.getByName("release") }
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
        compose = true
        buildConfig = true
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    // ui-tooling is debug-only so Android Studio's Layout Inspector can render the tree.
    // ui-tooling-preview (the @Preview annotation library) is deliberately omitted — the
    // codebase doesn't declare any @Preview composables. Add it back when one is added.
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.navigation.compose)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.coil.compose)

    implementation(libs.google.maps.compose)
    implementation(libs.google.play.services.maps)
    implementation(libs.google.places)

    implementation(libs.androidx.datastore.preferences)

    implementation(libs.reorderable)

    implementation(libs.kotlinx.serialization.json)
}
