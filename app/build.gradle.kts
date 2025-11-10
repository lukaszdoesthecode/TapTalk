import org.gradle.testing.jacoco.tasks.JacocoReport
import org.gradle.testing.jacoco.plugins.JacocoTaskExtension

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("kotlin-kapt")
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.0"
    alias(libs.plugins.google.gms.google.services)
    id("jacoco")
}

android {
    namespace = "com.example.taptalk"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.taptalk"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }

    buildFeatures { compose = true }

    composeOptions { kotlinCompilerExtensionVersion = "1.5.10" }

    sourceSets["main"].assets.srcDirs("src/main/assets")
}

dependencies {
    // --- Android Core ---
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    // --- Jetpack Compose ---
    implementation(platform("androidx.compose:compose-bom:2024.08.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // --- Firebase ---
    implementation(platform("com.google.firebase:firebase-bom:33.3.0"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-storage-ktx")

    // --- Coroutines ---
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")

    // --- Room ---
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")

    // --- Lifecycle / Work ---
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.6")
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    // --- Image Loading ---
    implementation("io.coil-kt:coil:2.7.0")
    implementation("io.coil-kt:coil-compose:2.7.0")

    // --- ML Kit ---
    implementation("com.google.mlkit:smart-reply:17.0.2")

    // --- Reorderable AAC Grid ---
    implementation("org.burnoutcrew.composereorderable:reorderable:0.9.6")

    // --- Testing ---
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test:core:1.5.0")
    androidTestImplementation("androidx.test:runner:1.5.2")
    androidTestImplementation("androidx.test:rules:1.5.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

    // --- Mockito ---
    androidTestImplementation("org.mockito:mockito-android:5.12.0")
    androidTestImplementation("org.mockito.kotlin:mockito-kotlin:5.2.1")

    // --- Coroutines testing ---
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    androidTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
}
        jacoco {
            toolVersion = "0.8.11"
        }

tasks.withType<Test>().configureEach {
    useJUnit()

    // Correct Kotlin DSL way to configure Jacoco test task
    extensions.configure(JacocoTaskExtension::class) {
        isIncludeNoLocationClasses = true
        excludes = listOf("jdk.internal.*")
    }
}


tasks.register<JacocoReport>("jacocoTestReport") {
    dependsOn("testDebugUnitTest")
    group = "Reporting"
    description = "Generate JaCoCo coverage report for debug unit tests."

    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(false)
    }

    val kotlinDebugTree = fileTree(
        mapOf(
            "dir" to "$buildDir/tmp/kotlin-classes/debug",
            "exclude" to listOf(
                "**/R.class",
                "**/R$*.class",
                "**/BuildConfig.*",
                "**/Manifest*.*",
                "**/*Test*.*",
                "androidx/**",
                "com/google/**"
            )
        )
    )

    val javaDebugTree = fileTree(
        mapOf(
            "dir" to "$buildDir/intermediates/javac/debug/classes",
            "exclude" to listOf(
                "**/R.class",
                "**/R$*.class",
                "**/BuildConfig.*",
                "**/Manifest*.*",
                "**/*Test*.*",
                "androidx/**",
                "com/google/**"
            )
        )
    )

    classDirectories.setFrom(files(kotlinDebugTree, javaDebugTree))
    sourceDirectories.setFrom(files("src/main/java", "src/main/kotlin"))
    executionData.setFrom(
        fileTree(
            mapOf(
                "dir" to "$buildDir",
                "includes" to listOf(
                    "jacoco/testDebugUnitTest.exec",
                    "outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec"
                )
            )
        )
    )
}
