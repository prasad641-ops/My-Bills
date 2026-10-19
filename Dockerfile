# ---- 1) build the React frontend ----
FROM node:20-alpine AS frontend
WORKDIR /fe
COPY frontend/package*.json ./
RUN npm install
COPY frontend/ .
RUN npm run build

# ---- 2) build the Spring Boot jar (React build is served from /static) ----
FROM maven:3.9-eclipse-temurin-17 AS backend
WORKDIR /be
COPY backend/pom.xml .
RUN mvn -q dependency:go-offline
COPY backend/src ./src
COPY --from=frontend /fe/dist ./src/main/resources/static
RUN mvn -q package -DskipTests

# ---- 3) small runtime image ----
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=backend /be/target/billing-1.0.0.jar app.jar
ENV JAVA_TOOL_OPTIONS="-Xmx300m"
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
