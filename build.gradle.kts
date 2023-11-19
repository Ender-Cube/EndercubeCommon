plugins {
    id("java-library")
    id("maven-publish")
}

group = "net.endercube"
version = "3.0.0"

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

    // Configuration API
    implementation("org.spongepowered:configurate-hocon:4.1.2")

    // Kyori stuff (Adventure)
    implementation("net.kyori:adventure-text-minimessage:4.13.1")

    // Redis (Jedis)
    implementation("redis.clients:jedis:5.0.2")
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

tasks {
    javadoc {
        options {
            (this as CoreJavadocOptions).addStringOption("Xdoclint:none", "-quiet")
        }
    }
}