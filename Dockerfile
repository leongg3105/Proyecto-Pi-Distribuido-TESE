FROM eclipse-temurin:17-jdk-focal

WORKDIR /app
COPY CoordinadorPi.java .
RUN javac CoordinadorPi.java
CMD ["java", "CoordinadorPi"]
