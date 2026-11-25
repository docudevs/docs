rootProject.name = "docs-java-examples"

providers.gradleProperty("sdkRoot").orNull?.let { sdkRoot ->
    includeBuild("$sdkRoot/docudevs-java-sdk")
}
