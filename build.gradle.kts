plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.spotless)
}

// The Android style is set by `ktlint_code_style = android_studio` in .editorconfig.
// ktlint 1.x dropped the old `android` boolean property, so there is no editorConfigOverride here.
val composeRuleSet = "io.nlopez.compose.rules:ktlint:${libs.versions.composeRules.get()}"

spotless {
    kotlin {
        target("**/src/**/*.kt")
        targetExclude("**/build/**")
        ktlint(libs.versions.ktlint.get())
        // .customRuleSets(listOf(composeRuleSet))
        trimTrailingWhitespace()
        endWithNewline()
    }
    kotlinGradle {
        target("**/*.gradle.kts")
        targetExclude("**/build/**")
        ktlint(libs.versions.ktlint.get())
        trimTrailingWhitespace()
        endWithNewline()
    }
}

// The project gate. `verify` fixes formatting for you; `verifyCi` fails on it instead.
// Everything else the two run is identical.
//
// Task ordering is intentionally left unconstrained: Spotless only rewrites whitespace,
// import order, and unused imports, none of which change semantics. Cross-project
// parallelism is off (see gradle.properties), so nothing reads a file while it is
// being rewritten.
val gateTasks = listOf(":app:lintDebug", ":app:testDebugUnitTest", ":app:assembleDebug")

tasks.register("verify") {
    group = "verification"
    description = "Local gate: format, lint, test, assemble."
    dependsOn("spotlessApply")
    dependsOn(gateTasks)
}

tasks.register("verifyCi") {
    group = "verification"
    description = "CI gate: identical checks, but fails on formatting instead of fixing it."
    dependsOn("spotlessCheck")
    dependsOn(gateTasks)
}
