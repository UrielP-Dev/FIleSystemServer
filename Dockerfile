FROM ubuntu:latest
FROM openjdk:17-jdk-alpine
LABEL authors="Uriel-PP"
EXPOSE 8090
ENV SPRING_DATA_MONGODB_URI="mongodb+srv://root:root@cluster0.xcojy.mongodb.net/filesystem?retryWrites=true&w=majority&appName=Cluster0"
RUN mkdir -p /app/uploads && chmod 777 /app/uploads
ADD ./target/demo-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]