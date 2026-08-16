FROM eclipse-temurin:21-jdk

WORKDIR /app

COPY src ./src
COPY web ./web

RUN javac -d out src/*.java

CMD ["java", "-cp", "out", "Main"]