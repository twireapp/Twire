import com.android.build.gradle.internal.api.BaseVariantOutputImpl

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-parcelize")
    id("io.sentry.android.gradle") version "5.3.0"
}

android {
    namespace = "com.perflyst.twire"
    compileSdk = 36
    ndkVersion = "25.0.8775105"

    defaultConfig {
        applicationId = "com.perflyst.twire"
        minSdk = 21
        targetSdk = 35
        versionCode = 537
        versionName = "2.12.2"

        vectorDrawables.useSupportLibrary = true

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            setProguardFiles(
                listOf(
                    getDefaultProguardFile("proguard-android.txt"),
                    "proguard-rules.pro"
                )
            )
            resValue("string", "app_name", "Twire")
            signingConfig = signingConfigs.getByName("debug")
        }
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-DEBUG"
            resValue("string", "app_name", "Twire Debug")
        }
    }

    applicationVariants.all {
        outputs.all {
            (this as BaseVariantOutputImpl).outputFileName = "Twire-${versionName}.apk"
        }
    }

    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }

    compileOptions {
        encoding = "UTF-8"
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    packaging {
        resources.excludes.add("dependencies.txt")
    }

    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")

    //AndroidX
    implementation("androidx.core:core:1.16.0-beta01")
    implementation("androidx.appcompat:appcompat:1.3.1")
    implementation("androidx.browser:browser:1.3.0")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.1")
    implementation("androidx.palette:palette:1.0.0")
    implementation("androidx.preference:preference:1.1.1")
    implementation("androidx.recyclerview:recyclerview:1.2.1")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    implementation("androidx.transition:transition:1.4.1")
    // https://developer.android.com/jetpack/androidx/releases/recyclerview
    implementation("androidx.viewpager2:viewpager2:1.1.0-beta01")
    implementation("com.google.android.material:material:1.4.0")
    implementation("androidx.media:media:1.6.0")

    //https://github.com/bumptech/glide/releases
    val glideVersion = "4.14.2"
    implementation("com.github.bumptech.glide:glide:$glideVersion")
    annotationProcessor("com.github.bumptech.glide:compiler:$glideVersion")
    //https://github.com/zjupure/GlideWebpDecoder
    implementation("com.github.zjupure:webpdecoder:2.6.$glideVersion")

    //https://github.com/balysv/material-ripple/blob/master/CHANGELOG.md
    implementation("com.balysv:material-ripple:1.0.2")

    //https://github.com/rey5137/material/releases
    implementation("com.github.rey5137:material:1.3.1")

    //https://github.com/StephenVinouze/MaterialNumberPicker/releases
    implementation("com.github.StephenVinouze:MaterialNumberPicker:1.1.0")

    //https://github.com/google/gson/blob/master/CHANGELOG.md
    implementation("com.google.code.gson:gson:2.8.7")

    //https://github.com/ozodrukh/CircularReveal/releases
    implementation("com.github.ozodrukh:CircularReveal:1.3.1")

    //https://github.com/afollestad/material-dialogs/releases?after=2.0.0-alpha01
    implementation("com.github.afollestad.material-dialogs:core:0.8.6.2@aar")
    implementation("com.github.afollestad.material-dialogs:commons:0.8.6.2@aar")

    //https://github.com/google/ExoPlayer/blob/release-v2/RELEASENOTES.md
    val media3 = "1.3.0"
    implementation("androidx.media3:media3-exoplayer:$media3")
    implementation("androidx.media3:media3-exoplayer-hls:$media3")
    implementation("androidx.media3:media3-ui:$media3")
    implementation("androidx.media3:media3-session:$media3")

    //https://github.com/square/okhttp
    implementation("com.squareup.okhttp3:okhttp:3.12.13")

    //https://github.com/JakeWharton/timber/
    implementation("com.jakewharton.timber:timber:5.0.1")

    //https://github.com/google/guava/releases
    api("com.google.guava:guava:31.0.1-android")

    //https://github.com/twitch4j/twitch4j
    implementation("com.github.twitch4j:twitch4j:1.23.0")
    implementation("io.github.xanthic.cache:cache-provider-androidx:0.6.2")

    //Testing
    testImplementation("junit:junit:4.13.2")
    val androidXTestVersion = "1.4.0"
    androidTestImplementation("androidx.test:runner:$androidXTestVersion")
    androidTestImplementation("androidx.test:rules:$androidXTestVersion")

    val parcelerVersion = "1.1.12"
    implementation("org.parceler:parceler-api:$parcelerVersion")
    annotationProcessor("org.parceler:parceler:$parcelerVersion")

    //https://github.com/chrisbanes/insetter/
    implementation("dev.chrisbanes.insetter:insetter:0.6.1")
}

configurations.all {
    resolutionStrategy {
        dependencySubstitution {
            substitute(module("io.github.openfeign:feign-core:13.5"))
                .using(module("com.github.samfundev.feign:feign-core:473f95b104"))
        }
    }
}

sentry {
    includeProguardMapping = false
}
