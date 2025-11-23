# Use Eclipse Temurin (modern OpenJDK replacement)
FROM eclipse-temurin:17-jdk-jammy
# Change this to match your JAR name
ARG JAR_FILE=build/libs/*.jar
COPY ./build/libs/app.jar app.jar 
# In your backend Dockerfile, add:
RUN mkdir -p /fileserver/uploads/images /fileserver/uploads/profiles
RUN chmod -R 755 /fileserver

ENTRYPOINT ["java", "-jar", "/app.jar"]
