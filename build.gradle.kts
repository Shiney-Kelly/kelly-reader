/*
 * Root build file: declares the plugins the modules use, without applying them here.
 */

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
}
