buildscript {
    val kotlinVersion: String by project

    repositories {
        mavenCentral()
    }

    dependencies {
        classpath(kotlin("gradle-plugin", version = kotlinVersion))
    }
}

group = "org.ktlib"
version = "0.0.1-SNAPSHOT"

repositories {
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
}

plugins {
    `java-library`
    `maven-publish`
}

apply(plugin = "kotlin")

dependencies {
    val kotlinVersion: String by project
    val kotestVersion: String by project
    val ktormVersion: String by project

    compileOnly(gradleApi())

    implementation("com.github.ktlib-org:core:0.1.14")
    implementation("io.github.oshai:kotlin-logging-jvm:5.1.0")
    implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")
    implementation("com.zaxxer:HikariCP:5.0.1")
    implementation("org.flywaydb:flyway-core:9.16.0")
    implementation("org.postgresql:postgresql:42.5.4")
    implementation("com.github.f4b6a3:uuid-creator:5.3.5")
    
    compileOnly("org.ktorm:ktorm-core:$ktormVersion")
    compileOnly("org.ktorm:ktorm-jackson:$ktormVersion")
    compileOnly("org.ktorm:ktorm-support-postgresql:$ktormVersion")
    compileOnly("org.ktorm:ktorm-support-mysql:$ktormVersion")

    testImplementation("org.ktorm:ktorm-core:$ktormVersion")
    testImplementation("org.ktorm:ktorm-jackson:$ktormVersion")
    testImplementation("org.ktorm:ktorm-support-postgresql:$ktormVersion")
    testImplementation("org.ktorm:ktorm-support-mysql:$ktormVersion")
    testImplementation("io.kotest:kotest-assertions-core:$kotestVersion")
    testImplementation("io.kotest:kotest-runner-junit5:$kotestVersion")
    testImplementation("io.mockk:mockk:1.13.4")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = "17"
    }
}

val test by tasks.getting(Test::class) {
    useJUnitPlatform { }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "org.ktlib"
            artifactId = "database"
            version = "0.1.0"

            from(components["java"])

            pom {
                name.set("ktlib-database")
                description.set("A library for database stuff")
                url.set("http://ktlib.org")
                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                developers {
                    developer {
                        id.set("aaronfreeman")
                        name.set("Aaron Freeman")
                        email.set("aaron@freeman.zone")
                    }
                }
                scm {
                    connection.set("scm:git:git@github.com:ktlib-org/database.git")
                    url.set("https://github.com/ktlib-org/database")
                }
            }
        }
    }
}
