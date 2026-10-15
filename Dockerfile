# ---- Stage 1: build the WAR (no Maven needed on your machine) ----
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -q -B clean package -DskipTests

# ---- Stage 2: run it on Tomcat 10.1 ----
FROM tomcat:10.1-jdk25
RUN rm -rf /usr/local/tomcat/webapps/*
COPY --from=build /app/target/tempo.war /usr/local/tomcat/webapps/ROOT.war
EXPOSE 8080
# Hosts like Render tell us which port to use through $PORT
CMD ["sh", "-c", "sed -i \"s/port=\\\"8080\\\"/port=\\\"${PORT:-8080}\\\"/\" /usr/local/tomcat/conf/server.xml && exec catalina.sh run"]
