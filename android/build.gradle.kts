// Top-level build file where you can add configuration options common to all sub-projects/modules.
// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    // your version-catalog aliases
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false

    // Firebase / Google Services Gradle plugin (required for google-services.json)
    id("com.google.gms.google-services") version "4.4.2" apply false
}
