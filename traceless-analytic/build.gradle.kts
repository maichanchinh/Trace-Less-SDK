plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.maven.publish)
}

val versionMajor = 2
val versionMinor = 0
val versionPatch = 0
val versionBuild = 0

val versionCode = versionMajor * 10000 + versionMinor * 1000 + versionPatch * 100 + versionBuild
val versionNameLibrary = "${versionMajor}.${versionMinor}.${versionPatch}"

val configGroupID = "io.github.maichanchinh"
val configArtifactId = "traceless-analytic"

mavenPublishing {
    publishToMavenCentral(automaticRelease = true)
    signAllPublications()

    coordinates(configGroupID, configArtifactId, versionNameLibrary)
    pom {
        name.set("Traceless Analytics SDK")
        description.set("Android analytics SDK for Firebase-first tracking")
        inceptionYear.set("2026")
        url.set("https://github.com/maichanchinh/SDKTraceLess")
        licenses {
            license {
                name.set("The Apache Software License, Version 2.0")
                url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
            }
        }
        developers {
            developer {
                id.value("maichanchinh")
                name.value("ChinhMC")
                email.value("maichanchinhls@gmail.com")
            }
        }
        scm {
            connection.value("scm:git@github.com/maichanchinh/SDKTraceLess.git")
            developerConnection.value("scm:git@github.com/maichanchinh/SDKTraceLess.git")
            url.value("https://github.com/maichanchinh/SDKTraceLess")
        }
    }
}

android {
    namespace = "com.app.traceless.analytic"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        minSdk = 26

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.timber)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
