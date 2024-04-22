plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "5.15.3"
  kotlin("plugin.spring") version "1.9.23"
  kotlin("jvm") version "1.9.22"
  kotlin("plugin.jpa") version "1.9.22"
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
  implementation("org.flywaydb:flyway-core")
  implementation("com.vladmihalcea:hibernate-types-60:2.21.1")
  runtimeOnly("org.postgresql:postgresql")
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
