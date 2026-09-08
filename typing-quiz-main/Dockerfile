# Stage 1: Build the application
FROM maven:3.8.5-openjdk-17 AS build
WORKDIR /app
COPY pom.xml ./
COPY .mvn .mvn
COPY mvnw ./
RUN chmod +x mvnw
RUN ./mvnw -B -e -DskipTests dependency:go-offline
COPY src ./src
RUN ./mvnw -B -DskipTests clean package

# Stage 2: Create the final image
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=build /app/target/typing-quiz-1.1.0.jar app.jar
EXPOSE 8080

# 设置环境变量默认值（生产环境应通过 -e 参数覆盖）
ENV JAVA_OPTS="-Dfile.encoding=UTF-8 -Dsun.jnu.encoding=UTF-8"
ENV MYSQL_HOST=localhost
ENV MYSQL_USER=typingquiz
ENV MYSQL_PASSWORD=default_password
ENV JWT_SECRET=change_in_production

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
