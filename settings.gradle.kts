pluginManagement {
    repositories {
        google()
        mavenCentral()
    }
    resolutionStrategy {
        eachPlugin {
            when (requested.id.id) {
                "com.android.application" -> useModule(
                    "com.android.tools.build:gradle:${requested.version ?: "9.2.1"}",
                )

                "org.jetbrains.kotlin.plugin.compose" -> useModule(
                    "org.jetbrains.kotlin:compose-compiler-gradle-plugin:${requested.version ?: "2.2.10"}",
                )
            }
        }
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "CalisBloomprintsPOS"
include(":app")
