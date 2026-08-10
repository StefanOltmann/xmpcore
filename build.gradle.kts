import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.detekt)
    alias(libs.plugins.kover)
    alias(libs.plugins.build.time.tracker)
    alias(libs.plugins.git.versioning)
    alias(libs.plugins.resources)
    alias(libs.plugins.gradle.versions)
    alias(libs.plugins.maven.publish)
}

repositories {
    google()
    mavenCentral()
}

val productName = "XMP Core for Kotlin Multiplatform"

description = productName
group = "de.stefan-oltmann"
version = "0.0.0"

gitVersioning.apply {

    refs {
        /* Main branch contains the current dev version */
        branch("main") {
            version = "\${commit.short}"
        }
        /* Release / tags have real version numbers */
        tag("v(?<version>.*)") {
            version = "\${ref.version}"
        }
    }

    /* Fallback if branch was not found (for feature branches) */
    rev {
        version = "\${commit.short}"
    }
}

buildTimeTracker {
    sortBy.set(com.asarkar.gradle.buildtimetracker.Sort.DESC)
}

detekt {
    source.from("src", "build.gradle.kts")
    config.setFrom("detekt.yml")
    allRules = true
    parallel = true
    ignoreFailures = true
}

kover {
    reports {
        verify {
            rule {
                minBound(95)
            }
        }
    }
}

kotlin {

    explicitApi()

    android {

        namespace = "de.stefan_oltmann.xmp"

        compileSdk = libs.versions.android.compile.sdk.get().toInt()

        minSdk = libs.versions.android.min.sdk.get().toInt()

        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }

        androidResources {
            enable = true
        }

        withHostTest {}
    }

    mingwX64("win") {
        binaries {
            executable(setOf(NativeBuildType.RELEASE)) {
                entryPoint = "de.stefan_oltmann.xmp.main"
            }
        }
    }

    linuxX64 {
        binaries {
            executable(setOf(NativeBuildType.RELEASE)) {
                entryPoint = "de.stefan_oltmann.xmp.main"
            }
        }
    }

    linuxArm64 {
        binaries {
            executable(setOf(NativeBuildType.RELEASE)) {
                entryPoint = "de.stefan_oltmann.xmp.main"
            }
        }
    }

    jvm {

        java {
            sourceCompatibility = JavaVersion.VERSION_11
            targetCompatibility = JavaVersion.VERSION_11
        }
    }

    js()

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs()

    @Suppress("UnusedPrivateMember") // False positive
    val commonMain = sourceSets.getByName("commonMain") {

        dependencies {

            /* Needed to parse XML and create a DOM Document */
            implementation(libs.xmlutil.core)
        }
    }

    @Suppress("UnusedPrivateMember", "UNUSED_VARIABLE") // False positive
    val commonTest = sourceSets.getByName("commonTest") {
        dependencies {

            /* Kotlin Test */
            implementation(kotlin("test"))

            /* Multiplatform file access */
            implementation(libs.kotlinx.io.core)

            /* Test resources */
            implementation(libs.resources)
        }
    }

    @Suppress("UnusedPrivateMember", "UNUSED_VARIABLE") // False positive
    val jvmMain = sourceSets.getByName("jvmMain")

    @Suppress("UnusedPrivateMember", "UNUSED_VARIABLE") // False positive
    val jvmTest = sourceSets.getByName("jvmTest") {
        dependencies {
            implementation(kotlin("test-junit"))
        }
    }

    val xcf = XCFramework()

    listOf(
        /* App Store */
        iosArm64(),
        /* Apple Silicon iOS Simulator */
        iosSimulatorArm64(),
        /* macOS Devices */
        macosArm64()
    ).forEach {

        it.binaries.executable(setOf(NativeBuildType.RELEASE)) {
            baseName = "xmpcore"
            entryPoint = "de.stefan_oltmann.xmp.main"
        }

        it.binaries.framework(setOf(NativeBuildType.RELEASE)) {
            baseName = "xmpcore"
            /* Part of the XCFramework */
            xcf.add(this)
        }
    }

    @Suppress("UnusedPrivateMember", "UNUSED_VARIABLE") // False positive
    val androidMain = sourceSets.getByName("androidMain")

    val posixMain = sourceSets.create("posixMain") {
        dependsOn(commonMain)
    }

    @Suppress("UnusedPrivateMember", "UNUSED_VARIABLE") // False positive
    val winMain = sourceSets.getByName("winMain") {
        dependsOn(posixMain)
    }

    @Suppress("UnusedPrivateMember", "UNUSED_VARIABLE") // False positive
    val linuxX64Main = sourceSets.getByName("linuxX64Main") {
        dependsOn(posixMain)
    }

    @Suppress("UnusedPrivateMember", "UNUSED_VARIABLE") // False positive
    val linuxArm64Main = sourceSets.getByName("linuxArm64Main") {
        dependsOn(posixMain)
    }

    val iosArm64Main = sourceSets.getByName("iosArm64Main")
    val iosSimulatorArm64Main = sourceSets.getByName("iosSimulatorArm64Main")
    val macosArm64Main = sourceSets.getByName("macosArm64Main")

    @Suppress("UnusedPrivateMember", "UNUSED_VARIABLE") // False positive
    val appleMain = sourceSets.create("appleMain") {

        dependsOn(commonMain)
        dependsOn(posixMain)

        iosArm64Main.dependsOn(this)
        iosSimulatorArm64Main.dependsOn(this)
        macosArm64Main.dependsOn(this)
    }

    val iosArm64Test = sourceSets.getByName("iosArm64Test")
    val iosSimulatorArm64Test = sourceSets.getByName("iosSimulatorArm64Test")
    val macosArm64Test = sourceSets.getByName("macosArm64Test")

    @Suppress("UnusedPrivateMember", "UNUSED_VARIABLE") // False positive
    val appleTest = sourceSets.create("appleTest") {

        dependsOn(commonTest)

        iosArm64Test.dependsOn(this)
        iosSimulatorArm64Test.dependsOn(this)
        macosArm64Test.dependsOn(this)
    }
}

// region Writing version.txt for GitHub Actions
val writeVersion = tasks.register("writeVersion") {
    doLast {
        File("build/version.txt").writeText(project.version.toString())
    }
}

tasks.getByPath("build").finalizedBy(writeVersion)
// endregion

// region Maven publish

val javadocJar = tasks.register<Jar>("javadocJar") {
    archiveClassifier.set("javadoc")
}

val signingEnabled: Boolean = System.getenv("SIGNING_ENABLED")?.toBoolean() ?: false

mavenPublishing {

    publishToMavenCentral()

    if (signingEnabled)
        signAllPublications()

    coordinates(
        groupId = "de.stefan-oltmann",
        artifactId = "xmpcore",
        version = version.toString()
    )

    pom {

        name = productName
        description = "XMP Core for Kotlin Multiplatform"
        url = "https://github.com/StefanOltmann/xmpcore"

        licenses {
            license {
                name = "The BSD License"
                url = "https://github.com/StefanOltmann/xmpcore/blob/main/original_source/original_license.txt"
            }
        }

        developers {
            developer {
                name = "Stefan Oltmann"
                url = "https://stefan-oltmann.de/"
                roles = listOf("maintainer", "developer")
                properties = mapOf("github" to "StefanOltmann")
            }
        }

        scm {
            url = "https://github.com/StefanOltmann/xmpcore"
            connection = "scm:git:git://github.com/StefanOltmann/xmpcore.git"
        }
    }
}
// endregion
