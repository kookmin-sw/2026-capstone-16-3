import java.util.Properties

plugins {
    id("com.android.application")
    id("kotlin-android")
    // The Flutter Gradle Plugin must be applied after the Android and Kotlin Gradle plugins.
    id("dev.flutter.flutter-gradle-plugin")
}

android {
    namespace = "com.retriever.gilbeot"
    compileSdk = flutter.compileSdkVersion
    ndkVersion = flutter.ndkVersion

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_11.toString()
    }

    defaultConfig {
        // TODO: Specify your own unique Application ID (https://developer.android.com/studio/build/application-id.html).
        applicationId = "com.retriever.gilbeot"
        // You can update the following values to match your application needs.
        // For more information, see: https://flutter.dev/to/review-gradle-config.
        minSdk = flutter.minSdkVersion
        targetSdk = flutter.targetSdkVersion
        versionCode = flutter.versionCode
        versionName = flutter.versionName

        val localProps = Properties()
        val localPropsFile = rootProject.file("local.properties")
        if (localPropsFile.exists()) localProps.load(localPropsFile.inputStream())
        manifestPlaceholders["kakaoNativeAppKey"] = localProps.getProperty("kakaoNativeAppKey", "")
    }

    val localProps = Properties()
    val localPropsFile = rootProject.file("local.properties")
    if (localPropsFile.exists()) localProps.load(localPropsFile.inputStream())

    signingConfigs {
        create("release") {
            val sf = localProps.getProperty("storeFile")
            if (sf != null) {
                storeFile = file(sf)
                storePassword = localProps.getProperty("storePassword", "")
                keyAlias = localProps.getProperty("keyAlias", "")
                keyPassword = localProps.getProperty("keyPassword", "")
            }
        }
    }

    buildTypes {
        release {
            val sf = localProps.getProperty("storeFile")
            signingConfig = if (sf != null) signingConfigs.getByName("release")
                            else signingConfigs.getByName("debug")
        }
    }
}

flutter {
    source = "../.."
}
