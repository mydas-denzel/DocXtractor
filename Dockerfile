FROM eclipse-temurin:21-jre-alpine
AS build
WORKDIR /app

COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
RUN ./mvnw dependency:go-offline

COPY src ./src
RUN ./mvnw -DskipTests package

# ---------------------------
# Runtime stage
# ---------------------------
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# install tesseract + language packs
RUN apt-get update && apt-get install -y \
    tesseract-ocr \
    tesseract-ocr-eng \
    libtesseract-dev \
    libleptonica-dev \
    && apt-get clean && rm -rf /var/lib/apt/lists/*

COPY --from=build /app/target/*.jar app.jar

ENV TESSDATA_PREFIX=/usr/share/tesseract-ocr/5/tessdata/

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
