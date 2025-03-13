FROM openjdk:17

RUN groupadd --system spring && useradd --system -g spring spring
USER spring:spring

#ARG JAR_FILE=build/libs/*.jar
#COPY ${JAR_FILE} app.jar
#
#ENTRYPOINT ["java", "-jar", "/app.jar"]

ARG DEPENDENCY=build/dependency
COPY ${DEPENDENCY}/BOOT-INF/lib /app/lib
COPY ${DEPENDENCY}/META-INF /app/META-INF
COPY ${DEPENDENCY}/BOOT-INF/classes /app

# 設定 "/app/templates/" 目錄
RUN mkdir -p /app/templates

# 設定 "/app/templates/" 目錄為 Volume，允許掛載外部資料
VOLUME /app/templates

ENTRYPOINT ["java","-cp","app:app/lib/*","com.line.bank.bxi.bpm.elf.backend.Application"]