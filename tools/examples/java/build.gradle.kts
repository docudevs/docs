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
    implementation("ai.docudevs:docudevs-java-sdk:0.4.1")
}

sourceSets {
    main {
        java {
            setSrcDirs(listOf("../../../examples"))
            include("**/Example.java")
        }
    }
}
