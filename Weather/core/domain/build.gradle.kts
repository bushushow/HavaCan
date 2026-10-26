// HavaCan's models and rules: plain Kotlin on the JVM, no Android, Room or Compose. Keeping them in
// their own module turns "domain stays pure" from a convention into something the compiler checks.
plugins {
    alias(libs.plugins.kotlin.jvm)
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
    }
}

dependencies {
    // Only for @SerializedName on City, whose field names follow the Open-Meteo response
    implementation(libs.gson)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
