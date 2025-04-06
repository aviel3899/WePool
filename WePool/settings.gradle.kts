pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }

    versionCatalogs {
        create("libs") {
          //  from(files("gradle/libs.versions.toml"))
            // אם קובץ ה-TOML שלך נמצא במיקום ברירת המחדל (gradle/libs.versions.toml), Gradle טוען אותו אוטומטית גם אם אין קריאת from() מפורשת בתוך בלוק ה-libs ב-settings.gradle(.kts).
        }
    }
}

rootProject.name = "WePool"
include(":app")
