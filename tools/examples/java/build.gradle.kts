plugins {
    java
}

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

dependencies {
    implementation("ai.docudevs:docudevs-java-sdk:0.3.0")
}

sourceSets {
    main {
        java {
            setSrcDirs(listOf("../../../examples"))
            include("**/Example.java")
        }
    }
}
