# Use a base image with Java
FROM eclipse-temurin:21-jdk-alpine
COPY target/*.jar Rahwan.jar
EXPOSE 8089
ENTRYPOINT ["java","-jar","Rahwan.jar"]