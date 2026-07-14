plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("maven-publish")
}

android {
    namespace = "io.igrant.stackview.compose"
    compileSdk = 34

    defaultConfig {
        minSdk = 24
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    // Re-export StackConfig so consumers of `io.igrant:stackview-compose` get it transitively.
    api(project(":stackview-core"))

    val composeBom = platform("androidx.compose:compose-bom:2024.09.00")
    api(composeBom)
    api("androidx.compose.foundation:foundation")
    api("androidx.compose.ui:ui")
    implementation("androidx.compose.animation:animation")

    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.compose.ui:ui-tooling-preview")

    testImplementation("junit:junit:4.13.2")
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])

                groupId = "io.igrant"
                artifactId = "stackview-compose"
                version = findProperty("VERSION_NAME") as String? ?: "1.0.0"

                pom {
                    name.set("StackView Compose")
                    description.set("A Jetpack Compose implementation of the wallet-style stacked card view")
                    url.set("https://github.com/L3-iGrant/android-stack-view")

                    licenses {
                        license {
                            name.set("The Apache License, Version 2.0")
                            url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                        }
                    }
                }
            }
        }

        repositories {
            maven {
                name = "GitHubPackages"
                url = uri("https://maven.pkg.github.com/L3-iGrant/android-stack-view")
                credentials {
                    username = System.getenv("GITHUB_ACTOR") ?: findProperty("gpr.user") as String? ?: ""
                    password = System.getenv("GITHUB_TOKEN") ?: findProperty("gpr.token") as String? ?: ""
                }
            }
        }
    }
}
