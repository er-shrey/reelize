import com.android.build.api.artifact.SingleArtifact

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.shrey.reelize"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.shrey.reelize"
        minSdk = 30
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            // Temporary: signed with the debug key so release builds are installable
            // for testing. Replace with a real release keystore before sharing widely
            // (same approach as Album Keeper).
            signingConfig = signingConfigs.getByName("debug")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    sourceSets {
        getByName("main").kotlin.srcDir("src/main/kotlin")
        getByName("test").kotlin.srcDir("src/test/kotlin")
        getByName("androidTest").kotlin.srcDir("src/androidTest/kotlin")
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    lint {
        abortOnError = true
        warningsAsErrors = false
        error += setOf("NewApi", "MissingPermission")
    }

    packaging {
        resources.excludes += setOf("/META-INF/{AL2.0,LGPL2.1}")
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
    testImplementation(libs.truth)
    testImplementation(libs.kotlinx.coroutines.test)

    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.truth)
    androidTestImplementation(libs.kotlinx.coroutines.test)
}

// Guardrail: the no-network guarantee is a product promise (C1),
// so a dependency must never be able to reintroduce INTERNET via manifest merging.
//
// Wired through the AGP artifacts API rather than by reading the intermediates directory
// by hand. That gives the check a real task dependency on manifest processing, so it can
// never run before the manifest exists and pass on nothing.
val assertNoInternetPermission = tasks.register("assertNoInternetPermission") {
    group = "verification"
    description = "Fails if android.permission.INTERNET appears in a merged manifest."
}

androidComponents {
    onVariants { variant ->
        val merged = variant.artifacts.get(SingleArtifact.MERGED_MANIFEST)
        val name = variant.name.replaceFirstChar { it.uppercase() }
        val check = tasks.register("assertNoInternetPermission$name") {
            group = "verification"
            description = "Fails if INTERNET appears in the $name merged manifest."
            val manifest = merged
            inputs.file(manifest).withPropertyName("mergedManifest")
            doLast {
                val file = manifest.get().asFile
                if (file.readText().contains("android.permission.INTERNET")) {
                    throw GradleException(
                        "C1 VIOLATION: android.permission.INTERNET found in " + file.path,
                    )
                }
                logger.lifecycle("C1 OK: no INTERNET permission in the $name manifest.")
            }
        }
        assertNoInternetPermission.configure { dependsOn(check) }
    }
}

// Guardrail: networking and analytics libraries are prohibited,
// not merely unused, because each would pull INTERNET back in.
val assertNoNetworkDependencies = tasks.register("assertNoNetworkDependencies") {
    group = "verification"
    description = "Fails if a networking or analytics library is on the runtime classpath."
    val forbidden = listOf(
        "com.squareup.okhttp3", "com.squareup.retrofit2", "com.google.firebase",
        "io.ktor", "com.google.android.gms:play-services-analytics",
        "com.google.android.gms:play-services-measurement", "com.amplitude", "com.mixpanel",
    )
    val artifacts = configurations.named("releaseRuntimeClasspath").map { config ->
        config.incoming.resolutionResult.allComponents.map { it.id.displayName }
    }
    doLast {
        val hits = artifacts.get().filter { id -> forbidden.any { id.startsWith(it) } }
        if (hits.isNotEmpty()) {
            throw GradleException(
                "C1 VIOLATION: prohibited network/analytics dependency:\n" +
                    hits.joinToString("\n") { "  $it" },
            )
        }
        logger.lifecycle("C1 OK: no network or analytics dependencies.")
    }
}

tasks.named("check") {
    dependsOn(assertNoNetworkDependencies)
}

tasks.named("check") {
    dependsOn(assertNoInternetPermission)
}
