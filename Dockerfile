FROM eclipse-temurin:17-jdk AS build

WORKDIR /app
EXPOSE 8081

COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

RUN sed -i 's/\r$//' mvnw && chmod +x mvnw
RUN ./mvnw dependency:go-offline -B

COPY src src
RUN ./mvnw package -DskipTests

ENTRYPOINT ["java","-jar","/app/target/data-service-0.0.1-SNAPSHOT.jar"]
