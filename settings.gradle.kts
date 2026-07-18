pluginManagement {
    repositories {
        google()
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
}

rootProject.name = "UygulamamDil"

include(":android:app")
include(":android:core:model")
include(":android:core:designsystem")
include(":android:core:data")
include(":android:core:network")
include(":android:feature:home")
include(":android:feature:lesson")
include(":android:feature:library")
include(":android:feature:profile")
include(":android:feature:onboarding")

include(":backend:domain")
include(":backend:application")
include(":backend:infrastructure:deepseek")
include(":backend:infrastructure:source")
include(":backend:infrastructure:supabase")
include(":backend:api")
