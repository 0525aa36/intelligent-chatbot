plugins {
	kotlin("jvm") version "1.9.25"
	kotlin("plugin.spring") version "1.9.25"
	id("org.springframework.boot") version "3.5.9"
	id("io.spring.dependency-management") version "1.1.7"
	kotlin("plugin.jpa") version "1.9.25"
}

group = "com.jaemin"
version = "0.0.1-SNAPSHOT"
description = "RAG-based AI Chatbot Service with Kotlin and Spring"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(17)
	}
}

// OpenAI 의존성 제거로 인해 Spring AI BOM 주석 처리
// extra["springAiVersion"] = "1.0.0-M5"
//
// dependencyManagement {
// 	imports {
// 		mavenBom("org.springframework.ai:spring-ai-bom:${property("springAiVersion")}")
// 	}
// }

repositories {
	mavenCentral()
	maven { url = uri("https://repo.spring.io/milestone") }
	maven { url = uri("https://repo.spring.io/snapshot") }
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.springframework.boot:spring-boot-starter-webflux")
	implementation("org.springframework.boot:spring-boot-starter-security")
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
	implementation("org.jetbrains.kotlin:kotlin-reflect")

	// Spring AI - OpenAI 대신 Gemini 사용으로 주석 처리
	// implementation("org.springframework.ai:spring-ai-openai-spring-boot-starter")

	// JWT
	implementation("io.jsonwebtoken:jjwt-api:0.12.3")
	runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.3")
	runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.3")
	
	// CSV
	implementation("org.apache.commons:commons-csv:1.10.0")
	
	developmentOnly("org.springframework.boot:spring-boot-docker-compose")
	runtimeOnly("org.postgresql:postgresql")
	
	// Test
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.springframework.security:spring-security-test")
	testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
	testImplementation("io.mockk:mockk:1.13.8")
	testImplementation("com.ninja-squad:springmockk:4.0.2")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
	testRuntimeOnly("com.h2database:h2")
	implementation(kotlin("stdlib-jdk8"))
}

kotlin {
	compilerOptions {
		freeCompilerArgs.addAll("-Xjsr305=strict")
	}
}

allOpen {
	annotation("jakarta.persistence.Entity")
	annotation("jakarta.persistence.MappedSuperclass")
	annotation("jakarta.persistence.Embeddable")
}

tasks.withType<Test> {
	useJUnitPlatform()
}
