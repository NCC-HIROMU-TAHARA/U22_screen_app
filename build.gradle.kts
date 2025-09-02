// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.androidApplication) apply false
    // ▼▼▼ この行を修正 ▼▼▼
    alias(libs.plugins.kotlinAndroid) apply false
}