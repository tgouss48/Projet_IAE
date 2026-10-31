FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q -B dependency:go-offline || true
COPY src ./src
RUN mvn -q -B -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/rail-test-client.jar app.jar
ENV IOR_FILE=/ior/RailBooking.ior
ENTRYPOINT ["java", "-jar", "app.jar"]
