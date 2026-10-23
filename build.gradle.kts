import org.gradle.api.tasks.Delete

// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    repositories {
        google()
        mavenCentral() // Replaced jcenter()
    }
    dependencies {
        classpath ("com.android.tools.build:gradle:9.0.1")
        classpath ("org.jetbrains.kotlin:kotlin-gradle-plugin:2.2.10")
        classpath("org.jetbrains.kotlin:compose-compiler-gradle-plugin:2.2.10")

        // NOTE: Do not place your application dependencies here; they belong
        // in the individual module build.gradle files
    }
}

allprojects {
    repositories {
        google()
        mavenCentral() // Replaced jcenter()
    }
}

tasks.register("clean",Delete::class.java) {
    delete(rootProject.buildDir)
}
