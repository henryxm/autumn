FROM eclipse-temurin:23-jdk-alpine

# Install required tools and fonts
RUN apk --no-cache add curl bash ttf-dejavu

EXPOSE 80
VOLUME /tmp

ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} app.jar

ENTRYPOINT ["java", "-jar", "-Duser.timezone=Asia/Shanghai", "/app.jar"]
