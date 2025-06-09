# 1. 베이스 이미지 선택
# Java 21이 필요하므로, OpenJDK 21을 사용하는 경량화된 이미지 (예: temurin)를 선택합니다.
# builder 단계에서 사용할 이미지 (애플리케이션 빌드를 위한 JDK 포함)
FROM eclipse-temurin:21-jdk-alpine AS builder

# 2. 작업 디렉토리 설정
WORKDIR /app

# 3. Gradle 빌드 파일 복사
# 프로젝트의 Gradle 빌드 관련 파일을 복사하여 의존성을 먼저 다운로드하고 빌드합니다.
# 이는 빌드 캐싱을 활용하여 Docker 빌드 시간을 단축하는 데 도움이 됩니다.
COPY build.gradle settings.gradle ./
COPY gradlew ./
COPY gradle ./gradle

# Dockerfile 내에서 gradlew 실행 권한 부여
RUN chmod +x gradlew

# 4. Gradle 의존성 다운로드 및 빌드
# 먼저 의존성만 다운로드하여 캐시를 활용합니다.
RUN ./gradlew dependencies

# 5. 소스 코드 복사
# 의존성 다운로드 후 전체 소스 코드를 복사합니다.
COPY src ./src

# 6. 애플리케이션 빌드
# Spring Boot 프로젝트를 JAR 파일로 빌드합니다.
# clean build 대신 assemble이나 bootJar를 사용하는 것이 일반적이며, 여기서는 bootJar를 사용합니다.
RUN ./gradlew bootJar

# 7. 최종 실행 이미지 선택 (멀티스테이지 빌드)
# 실제 애플리케이션 실행을 위한 더 가벼운 JRE 이미지를 선택합니다.
# builder 이미지에서 생성된 JAR 파일만 복사하여 최종 이미지 크기를 최소화합니다.
FROM eclipse-temurin:21-jre-alpine

# 8. 작업 디렉토리 설정 (다시)
WORKDIR /app

# 9. 빌드된 JAR 파일 복사
# builder 단계에서 생성된 JAR 파일을 최종 이미지로 복사합니다.
# build/libs/your-application.jar 경로를 사용하며, your-application은 프로젝트명이나 build.gradle의 archiveBaseName에 따라 달라집니다.
# 보통 build.gradle 파일의 `bootJar` 설정에 따라 JAR 파일명이 결정됩니다.
# 예시: build/libs/villigo-0.0.1-SNAPSHOT.jar 또는 build/libs/villigo.jar
COPY --from=builder /app/build/libs/*.jar app.jar

# 10. 애플리케이션 실행 포트 노출 (선택 사항)
# 애플리케이션이 사용할 포트를 외부에 노출합니다. (Spring Boot 기본 포트 8080)
EXPOSE 8080

# 11. 애플리케이션 실행 명령어
# 컨테이너가 시작될 때 실행될 명령어입니다.
# -Dspring.profiles.active=prod와 같이 특정 프로파일을 지정할 수도 있습니다.
ENTRYPOINT ["java", "-Dspring.profiles.active=${SPRING_PROFILES_ACTIVE}", "-jar", "app.jar"]