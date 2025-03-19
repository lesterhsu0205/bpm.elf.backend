# 使用 bootJar, embeded tomcat 的打包方式
#FROM openjdk:17-jdk-alpine
#RUN addgroup -S spring && adduser -S spring -G spring


FROM registry.access.redhat.com/ubi8/openjdk-17-runtime

USER root
RUN groupadd -r spring && useradd -r -g spring spring

USER spring:spring

WORKDIR /app

#ARG JAR_FILE=build/libs/*.jar
#COPY ${JAR_FILE} app.jar
#
#ENTRYPOINT ["java", "-jar", "/app.jar"]

ARG DEPENDENCY=build/dependency
COPY ${DEPENDENCY}/BOOT-INF/lib /app/lib
COPY ${DEPENDENCY}/BOOT-INF/classes /app
COPY ${DEPENDENCY}/META-INF /app/META-INF

# 設定 "/app/templates/" 目錄
RUN mkdir -p /app/templates

# 設定 "/app/templates/" 目錄為 Volume，允許掛載外部資料
VOLUME /app/templates

ENTRYPOINT ["java","-cp","/app:/app/lib/*","com.line.bank.bxi.bpm.elf.backend.Application"]