# **DocXtractor API: Intelligent Document Processing 📄**

## Overview
DocXtractor is a robust Spring Boot application designed for intelligent document processing, leveraging advanced technologies to extract, analyze, and enrich information from various document types. It integrates with object storage for document persistence and a Large Language Model (LLM) for deep content analysis, streamlining information retrieval and categorization.

## Features
-   **Document Upload**: Securely uploads PDF, DOCX, and common image files.
-   **Text Extraction**: Extracts textual content from diverse document formats using Apache Tika, PDFBox, and Apache POI.
-   **OCR Integration**: Employs Tesseract OCR for accurate text recognition in scanned documents or image-based content within PDFs.
-   **LLM-Powered Analysis**: Utilizes OpenRouter to integrate various Large Language Models for advanced document classification (e.g., invoice, CV, report), summarization, and structured entity extraction (names, dates, amounts, emails, phones).
-   **Object Storage**: Persists uploaded documents in a scalable MinIO-compatible object storage solution.
-   **Metadata Management**: Stores and retrieves comprehensive metadata alongside extracted content and LLM analysis results in a relational database.
-   **RESTful API**: Provides a clean, well-documented REST API for seamless integration with other systems.
-   **Containerized Deployment**: Dockerfile included for easy and consistent deployment.

## Technologies Used
| Technology       | Description                                                 | Link                                             |
| :--------------- | :---------------------------------------------------------- | :----------------------------------------------- |
| `Spring Boot`    | Framework for building robust, production-ready Java applications. | [Spring Boot](https://spring.io/projects/spring-boot) |
| `Java 21`        | Modern, high-performance programming language.              | [Java](https://openjdk.org/projects/jdk/21/)     |
| `H2 Database`    | Lightweight, embedded relational database for local development. | [H2 Database](https://www.h2database.com/)       |
| `Spring Data JPA`| Simplifies database access with Hibernate ORM.              | [Spring Data JPA](https://spring.io/projects/spring-data-jpa) |
| `MinIO`          | High-performance, S3 compatible object storage.             | [MinIO](https://min.io/)                         |
| `Apache Tika`    | Content detection and analysis framework.                   | [Apache Tika](https://tika.apache.org/)          |
| `Tess4J`         | Java wrapper for Tesseract OCR engine.                      | [Tess4J](https://tess4j.sourceforge.net/)        |
| `Apache POI`     | Library for working with Microsoft Office formats (e.g., DOCX). | [Apache POI](https://poi.apache.org/)            |
| `PDFBox`         | Java library for PDF document manipulation.                 | [Apache PDFBox](https://pdfbox.apache.org/)      |
| `OpenRouter`     | Unified API for various Large Language Models.              | [OpenRouter](https://openrouter.ai/)             |
| `Lombok`         | Reduces boilerplate code for Java beans.                    | [Project Lombok](https://projectlombok.org/)     |
| `Springdoc OpenAPI`| Generates interactive API documentation (Swagger UI).   | [Springdoc](https://springdoc.org/)              |

## Getting Started
To get a local copy of DocXtractor up and running, follow these steps.

### Installation
1.  **Clone the Repository**:
    ```bash
    git clone https://github.com/mydas-denzel/DocXtractor.git
    cd DocXtractor
    ```
2.  **Build the Project**:
    Ensure you have Java Development Kit (JDK) 21 and Apache Maven installed.
    ```bash
    ./mvnw clean install
    ```
3.  **Run the Application**:
    ```bash
    ./mvnw spring-boot:run
    ```
    The application will start on `http://localhost:8080`.
    
    You can access the H2 console at `http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:file:./data/docxtractor`).
    
4.  **Docker (Optional) 🐳**:
    For a containerized setup, ensure Docker is installed.
    ```bash
    docker build -t docxtractor .
    docker run -p 8080:8080 docxtractor
    ```
    *Note: The Docker image includes Tesseract OCR and its language data for out-of-the-box text recognition.*
5.  **MinIO Setup (Local Development) 💾**:
    If you wish to run MinIO locally instead of using the remote endpoint, you can start it with Docker:
    ```bash
    docker run -p 9000:9000 -p 9001:9001 \
      -e "MINIO_ROOT_USER=minioadmin" \
      -e "MINIO_ROOT_PASSWORD=minioadmin" \
      quay.io/minio/minio server /data --console-address ":9001"
    ```
    Then, update the `storage.endpoint` in your `application.properties` to `http://localhost:9000`.

### Environment Variables
Configure the application by modifying `src/main/resources/application.properties` or by setting system environment variables. Below are the required variables with examples:

```properties
# --- Application Configuration ---
spring.application.name=DocXtractor
app.upload.max-bytes=5242880 # Maximum allowed file upload size in bytes (e.g., 5MB)

# --- H2 Database Configuration (for local development) ---
spring.datasource.url=jdbc:h2:file:./data/docxtractor;DB_CLOSE_ON_EXIT=FALSE;AUTO_RECONNECT=TRUE
spring.datasource.driverClassName=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
spring.jpa.hibernate.ddl-auto=update
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console

# --- MinIO / S3 Object Storage Configuration ---
storage.endpoint=http://minio-docxtractor.zeabur.app # Public endpoint for MinIO or local (e.g., http://localhost:9000)
storage.access-key=minio # Your MinIO access key (e.g., minioadmin)
storage.secret-key=teqUoC5XQDNf17rI3d0ZMO2Ky94Y6Rs8 # Your MinIO secret key (e.g., minioadmin)
storage.bucket=zeabur # Default bucket name for document storage
storage.region=us-east-1 # AWS region (MinIO supports any value here)
storage.use-path-style=true # Set to true for MinIO-compatible storage

# --- OpenRouter LLM Configuration ---
openrouter.api-key=sk-or-v1-xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx # Your OpenRouter API Key
openrouter.model=nvidia/nemotron-nano-9b-v2:free # Preferred LLM model identifier (e.g., openai/gpt-4o-mini, nvidia/nemotron-nano-9b-v2:free)
openrouter.url=https://openrouter.ai/api/v1/chat/completions # OpenRouter API base URL

# --- Tesseract OCR Configuration ---
ocr.tessdata-path=/usr/share/tesseract-ocr/5/tessdata/ # Path to Tesseract tessdata directory (ensure accessible in environment)
ocr.language=eng # Default language pack for OCR (e.g., eng for English)

# --- Springdoc OpenAPI/Swagger UI Configuration ---
springdoc.api-docs.enabled=true
springdoc.swagger-ui.enabled=true
```
*Note: If you plan to use PostgreSQL, uncomment the PostgreSQL dependency in `pom.xml` and replace the H2 database properties with your PostgreSQL connection details.*

## API Documentation
### Base URL
`http://localhost:8080/documents`

### OpenAPI UI
An interactive API documentation (Swagger UI) is available at: `http://localhost:8080/swagger-ui.html`

### Endpoints
#### POST /documents/upload
Uploads a document for storage and initiates initial text extraction.
**Request**:
`Content-Type: multipart/form-data`
The request body must contain a `file` part.
```
--<boundary>
Content-Disposition: form-data; name="file"; filename="document.pdf"
Content-Type: application/pdf

[Binary content of the file]
--<boundary>--
```
**Response**:
`200 OK`
```json
{
  "id": 1,
  "fileName": "my_document.pdf",
  "contentType": "application/pdf",
  "sizeBytes": 102400,
  "containsImages": true,
  "imageCount": 3,
  "message": "Uploaded and text extracted; call /documents/{id}/analyze to run LLM"
}
```
**Errors**:
-   `400 Bad Request`: Returned if no file is provided or if the file's size exceeds `app.upload.max-bytes`.
-   `413 Payload Too Large`: Specifically for when the uploaded file size exceeds the server's configured maximum.
-   `500 Internal Server Error`: An unexpected error occurred during the upload or text extraction process.

#### POST /documents/{id}/analyze
Triggers the LLM-powered analysis for a previously uploaded document identified by its ID.
**Request**:
`id` (Path Variable): `Long` - The unique identifier of the document to be analyzed.
No request body is required.
**Response**:
`200 OK`
```json
{
  "id": 1,
  "fileName": "my_document.pdf",
  "contentType": "application/pdf",
  "sizeBytes": 102400,
  "containsImages": true,
  "imageCount": 3,
  "message": "LLM analysis complete"
}
```
**Errors**:
-   `400 Bad Request`: If no document is found for the provided `id`.
-   `500 Internal Server Error`: An unexpected error occurred during the LLM analysis process.

#### GET /documents/{id}
Retrieves detailed information and LLM analysis results for a specific document.
**Request**:
`id` (Path Variable): `Long` - The unique identifier of the document to retrieve.
No request body is required.
**Response**:
`200 OK`
```json
{
  "id": 1,
  "fileName": "my_document.pdf",
  "contentType": "application/pdf",
  "sizeBytes": 102400,
  "storagePath": "zeabur/my_document-1700000000000-abcd12.pdf",
  "extractedText": "This is the extracted content from the document, including OCR results.",
  "containsImages": true,
  "imageCount": 3,
  "analyzed": true,
  "documentType": "invoice",
  "summary": "This document is an invoice from ABC Corp to XYZ Ltd for services rendered in Q1.",
  "metadataJson": "{\"names\":[\"ABC Corp\",\"XYZ Ltd\"],\"dates\":[\"2023-01-15\",\"2023-02-01\"],\"amounts\":[\"$1,500.00\"],\"emails\":[\"billing@abccorp.com\"],\"phones\":[\"+1-555-123-4567\"]}"
}
```
**Errors**:
-   `400 Bad Request`: If no document is found for the provided `id`.
-   `500 Internal Server Error`: An unexpected error occurred during document retrieval.

## Usage Examples
This API is designed for integrating intelligent document processing into backend workflows. Here's a typical interaction flow using `curl`:

1.  **Upload a Document (e.g., a sample invoice PDF)**:
    ```bash
    curl -v -X POST "http://localhost:8080/documents/upload" \
         -H "Accept: application/json" \
         -F "file=@/path/to/your/sample_invoice.pdf;type=application/pdf"
    ```
    *This command will return a `DocumentUploadResponse` JSON object, including the `id` of the newly uploaded document (e.g., `123`).*

2.  **Initiate LLM Analysis for the Uploaded Document**:
    *Assuming the `id` from the upload response was `123`.*
    ```bash
    curl -v -X POST "http://localhost:8080/documents/123/analyze" \
         -H "Accept: application/json"
    ```
    *This triggers the asynchronous LLM analysis and returns another `DocumentUploadResponse` indicating that the analysis is complete.*

3.  **Retrieve Detailed Document Information and Analysis Results**:
    *Using the same document `id` (`123`).*
    ```bash
    curl -v -X GET "http://localhost:8080/documents/123" \
         -H "Accept: application/json"
    ```
    *The response will contain the `extractedText`, `documentType`, `summary`, and structured `metadataJson` generated by the LLM, along with other document details.*

## Contributing
Contributions are highly encouraged! If you have suggestions for improvements, new features, or bug fixes, please follow these guidelines:

*   ✨ Fork the repository to your GitHub account.
*   🌟 Create a new branch for your feature or fix (e.g., `git checkout -b feature/new-llm-model`).
*   📝 Implement your changes and ensure all existing tests pass, adding new tests where applicable.
*   🚀 Commit your changes with clear, descriptive messages (`git commit -m 'feat: Implement support for XYZ document type'`).
*   ⬆️ Push your branch to your forked repository (`git push origin feature/new-llm-model`).
*   ➡️ Open a Pull Request against the `master` branch of this repository, providing a detailed description of your changes.

Please ensure your code adheres to the project's coding standards and includes comprehensive test coverage.

## License
This project is currently without a specified license. Please feel free to add one that suits your needs.

## Author Info
*   **Developer**: [Your Name/Handle]
*   **LinkedIn**: [Your LinkedIn Profile URL]
*   **Portfolio**: [Your Portfolio Website URL]

---
[![Build Status](https://img.shields.io/badge/Build-Passing-brightgreen?style=flat-square)](https://github.com/mydas-denzel/DocXtractor/actions)
[![Language](https://img.shields.io/badge/Language-Java%2021-blue?style=flat-square)](https://openjdk.org/projects/jdk/21/)
[![Framework](https://img.shields.io/badge/Framework-Spring%20Boot-green?style=flat-square)](https://spring.io/projects/spring-boot)
[![API Reference](https://img.shields.io/badge/API%20Reference-Swagger%20UI-orange?style=flat-square)](http://localhost:8080/swagger-ui.html)
[![License: Unlicensed](https://img.shields.io/badge/License-Unlicensed-lightgrey.svg)](https://docs.github.com/en/repositories/managing-your-repositorys-settings-and-features/customizing-your-repository/licensing-a-repository#determining-where-your-repository-is-licensed)

[![Readme was generated by Dokugen](https://img.shields.io/badge/Readme%20was%20generated%20by-Dokugen-brightgreen)](https://www.npmjs.com/package/dokugen)