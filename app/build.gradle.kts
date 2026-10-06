plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
}
android {
  namespace = "com.naomichan.pos"
  compileSdk = 36
  defaultConfig {
    applicationId = "com.naomichan.pos"
    minSdk = 24
    targetSdk = 36
    versionCode = 10
    versionName = "3.0.0"
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }
  buildTypes {
    release {
      isMinifyEnabled = true
      isShrinkResources = true
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
    isCoreLibraryDesugaringEnabled = true
  }
  buildFeatures { compose = true; buildConfig = true }
  kotlin { compilerOptions { jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11 } }
}
dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.okhttp)
  implementation(libs.androidx.datastore.preferences)
  coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")
  implementation("com.sunmi:printerlibrary:1.0.24")
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
}
