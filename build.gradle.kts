import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.springframework.boot.gradle.tasks.run.BootRun

plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "6.0.9"
  kotlin("plugin.spring") version "2.0.21"
  kotlin("plugin.jpa") version "2.0.21"
  kotlin("jvm") version "2.0.21"
  id("org.jlleitschuh.gradle.ktlint") version "12.1.1"
  id("org.openapi.generator") version "7.9.0"
  jacoco
}

configurations {
  testImplementation { exclude(group = "org.junit.vintage") }
}

dependencies {
  implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.6.0")
  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("org.springframework.boot:spring-boot-starter-security")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
  implementation("org.springframework.cloud:spring-cloud-starter-openfeign:4.1.3")
  constraints {
    implementation("org.bouncycastle:bcprov-jdk18on:1.79") {
      because("1.77 has CVEs")
    }
  }
  implementation("org.springframework.cloud:spring-cloud-dependencies:2023.0.4")
  implementation("com.vladmihalcea:hibernate-types-60:2.21.1")
  implementation("org.flywaydb:flyway-core:10.21.0")
  runtimeOnly("org.flywaydb:flyway-database-postgresql:10.21.0")
  runtimeOnly("org.postgresql:postgresql")

  // Test dependencies
  testImplementation("com.ninja-squad:springmockk:4.0.2")
  testImplementation("io.jsonwebtoken:jjwt-impl:0.12.6")
  testImplementation("io.jsonwebtoken:jjwt-jackson:0.12.6")
  testImplementation("org.springframework.security:spring-security-test")

  // Dev dependencies
  developmentOnly("org.springframework.boot:spring-boot-devtools")
}

kotlin {
  jvmToolchain(21)
}

tasks {
  withType<KotlinCompile> {
    compilerOptions.jvmTarget = JvmTarget.JVM_21
  }

  withType<BootRun> {
    jvmArgs = listOf(
      "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005",
    )
  }
}
