repositories {
    System.getenv("MAVEN_CENTRAL_REPO").let { repo ->
        if (repo.isNullOrBlank())
            mavenCentral()
        else
            maven(url = repo)
    }
}

dependencies {
    implementation(libs.gson)
    implementation(libs.jna)
    implementation(libs.kala.compress.tar)
    implementation(libs.kala.compress.ar)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.processResources {
    into("org/zero/hmcl/gradle/l10n") {
        from(projectDir.resolve("../ZeroLauncherCore/src/main/resources/assets/lang/"))
    }
}
