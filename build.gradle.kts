plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "5.15.6"
  kotlin("plugin.spring") version "1.9.23"
  kotlin("jvm") version "1.9.22"
  kotlin("plugin.jpa") version "1.9.22"
  id("org.openapi.generator") version "5.4.0"
  id("org.jlleitschuh.gradle.ktlint") version "12.1.0"
  id("com.google.cloud.tools.jib") version "3.4.1"
  jacoco
}

configurations {
  testImplementation { exclude(group = "org.junit.vintage") }
}

dependencies {
  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.4.0")
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("org.springframework.boot:spring-boot-starter-security")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
  implementation("org.springframework.cloud:spring-cloud-starter-contract-stub-runner:4.0.2")
  implementation("org.springframework.cloud:spring-cloud-starter-openfeign:4.1.1")
  constraints {
    implementation("org.bouncycastle:bcprov-jdk18on:1.78.1") {
      because("1.77 has CVEs")
    }
  }
  implementation("org.springframework.cloud:spring-cloud-dependencies:2023.0.1")
  implementation("org.flywaydb:flyway-core")
  implementation("com.vladmihalcea:hibernate-types-60:2.21.1")
  runtimeOnly("org.postgresql:postgresql")
  testImplementation("com.ninja-squad:springmockk:4.0.2")
  testImplementation("io.jsonwebtoken:jjwt-impl:0.12.3")
  testImplementation("io.jsonwebtoken:jjwt-jackson:0.12.3")
}

kotlin {
  jvmToolchain(21)
}

tasks {
  withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
      jvmTarget = "21"
    }
  }
}
