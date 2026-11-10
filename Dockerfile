FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY pom.xml .
COPY src ./src
RUN mvn -B -DskipTests package
FROM eclipse-temurin:17-jre
WORKDIR /app
RUN useradd --system --create-home appuser
COPY --from=build /workspace/target/inventory-order-api-java-1.0.0.jar /app/app.jar
COPY --from=build /workspace/target/lib /app/lib
USER appuser
EXPOSE 8080
ENTRYPOINT ["java","--add-modules","jdk.httpserver","-jar","/app/app.jar"]
