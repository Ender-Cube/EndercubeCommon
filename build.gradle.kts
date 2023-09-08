plugins {
    id("java-library")
    id("maven-publish")
}

group = "net.endercube"
version = "1.1.1"

repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

dependencies {
    // Unit testing
    testImplementation("org.junit.jupiter:junit-jupiter")

    // Minestom
    implementation("dev.hollowcube:minestom-ce:010fe985bb")

    // HikariCP
    implementation("com.zaxxer:HikariCP:5.0.1")

    // MariaDB
    api("org.mariadb.jdbc:mariadb-java-client:3.1.4")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }

    withJavadocJar()
}

publishing {
    publications {
        create<MavenPublication>("EndercubeCommon") {
            from(components["java"])
        }
    }
}