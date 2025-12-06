# DocXtractor API

## Overview
DocXtractor is a robust Spring Boot application designed for comprehensive document processing and analysis. It integrates with various libraries to extract text and images from diverse file formats (PDF, DOCX, images) and leverages a Large Language Model (LLM) for advanced content summarization and entity extraction.

## Features
-   **Document Upload**: Securely upload documents (PDF, DOCX, JPG, PNG) to an object storage (MinIO).
-   **Text Extraction**: Utilize Apache Tika, PDFBox, Apache POI, and Tesseract OCR to extract text content from various document types, including handling image-based PDFs and scanned documents.
-   **Image Detection**: Identify and count images embedded within PDF and DOCX documents.
-   **LLM Integration**: Send extracted document content to an external Large Language Model (OpenRouter AI) for intelligent summarization, document type classification, and structured entity extraction (names, dates, amounts, emails, phones).
-   **Persistent Storage**: Store document metadata, extracted text, and LLM analysis results in a MySQL database using Spring Data JPA.
-   **RESTful API**: Expose clear and well-defined REST endpoints for document management and analysis.
-   **Swagger UI**: Automatic API documentation provided by Springdoc OpenAPI for easy interaction and testing.

## Getting Started

### Installation
To set up and run the DocXtractor backend locally, follow these steps:

1.  **Clone the Repository**:
    ```bash
    git clone https://github.com/mydas-denzel/DocXtractor.git
    cd DocXtractor
    ```

2.  **Prerequisites**:
    *   Java Development Kit (JDK) 21 or newer.
    *   Maven (included via `mvnw` wrapper).
    *   A running MySQL database instance.
    *   Access to a MinIO (or S3-compatible) object storage.
    *   Tesseract OCR installed on your system if you want to use the local OCR capabilities (e.g., `sudo apt-get install tesseract-ocr`). Ensure `tessdata` is available at the path specified in `application.properties`.

3.  **Build the Project**:
    ```bash
    ./mvnw clean install
    ```

4.  **Run the Application**:
    ```bash
    ./mvnw spring-boot:run
    ```
    The application will start on `http://localhost:8080` by default.

### Environment Variables
The application requires several environment variables for database, storage, and LLM configuration. These can be set in `src/main/resources/application.properties` or provided as system environment variables, with the latter overriding the former.

| Variable Name              | Example Value                                  | Description                                                                                                                                           |
| :------------------------- | :--------------------------------------------- | :---------------------------------------------------------------------------------------------------------------------------------------------------- |
| `spring.datasource.url`    | `jdbc:mysql://localhost:3306/docxtractor_db`   | JDBC URL for the MySQL database.                                                                                                                      |
| `spring.datasource.username` | `root`                                         | Username for database access.                                                                                                                         |
| `spring.datasource.password` | `password`                                     | Password for database access.                                                                                                                         |
| `storage.endpoint`         | `http://localhost:9000`                        | MinIO server endpoint.                                                                                                                                |
| `storage.access-key`       | `minioadmin`                                   | MinIO access key.                                                                                                                                     |
| `storage.secret-key`       | `minioadmin`                                   | Minio secret key.                                                                                                                                     |
| `storage.bucket`           | `docxtractor-bucket`                           | Default bucket name for storing documents.                                                                                                            |
| `storage.region`           | `us-east-1`                                    | AWS region for MinIO/S3 compatibility.                                                                                                                |
| `ocr.tessdata-path`        | `/usr/share/tessdata`                          | Path to Tesseract `tessdata` directory. Adjust based on your OS installation.                                                                         |
| `ocr.language`             | `eng`                                          | Tesseract language pack (e.g., `eng` for English).                                                                                                    |
| `app.upload.max-bytes`     | `5242880`                                      | Maximum allowed file size for uploads in bytes (e.g., 5MB).                                                                                           |
| `OPENROUTER_API_KEY`       | `sk-or-v1-xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx` | **Required**: API key for OpenRouter.ai service. This should be kept secure and ideally set as an environment variable.                               |
| `OPENROUTER_MODEL`         | `openai/gpt-4o-mini`                           | The specific LLM model to use from OpenRouter.ai (e.g., `openai/gpt-4o-mini`, `google/gemini-flash-1.5`).                                              |
| `openrouter.url`           | `https://api.openrouter.ai/v1/chat/completions`| Base URL for the OpenRouter API.                                                                                                                      |

## API Documentation
Access the interactive API documentation (Swagger UI) at `http://localhost:8080/swagger-ui.html` after the application starts.

### Base URL
`http://localhost:8080/api/v1`

### Endpoints

#### POST /api/v1/documents/upload
Uploads a document (PDF, DOCX, image) to storage and performs initial text/image extraction. This endpoint *does not* perform LLM analysis.

**Request**:
Requires `Content-Type: multipart/form-data`.
`file`: `MultipartFile` - The document file to upload.
```
Content-Type: multipart/form-data

--boundary
Content-Disposition: form-data; name="file"; filename="example.pdf"
Content-Type: application/pdf

[file content]
--boundary--
```

**Response**:
`200 OK`
```json
{
  "id": 1,
  "fileName": "example.pdf",
  "contentType": "application/pdf",
  "sizeBytes": 102400,
  "containsImages": true,
  "imageCount": 2,
  "message": "Uploaded and text extracted; call /documents/{id}/analyze to run LLM"
}
```

**Errors**:
-   `400 Bad Request`: "No file provided" or "Upload failed: [error message]"
-   `413 Payload Too Large`: "File exceeds allowed size"
-   `500 Internal Server Error`: "Internal error: [error message]"

#### POST /api/v1/documents/{id}/analyze
Triggers LLM analysis for a previously uploaded document identified by its ID.

**Request**:
No request body required.
```
POST /api/v1/documents/1/analyze
```

**Response**:
`200 OK`
```json
{
  "id": 1,
  "fileName": "example.pdf",
  "contentType": "application/pdf",
  "sizeBytes": 102400,
  "containsImages": true,
  "imageCount": 2,
  "message": "LLM analysis complete"
}
```

**Errors**:
-   `400 Bad Request`: "Document not found: [id]"
-   `500 Internal Server Error`: "Internal error: [error message]"

#### GET /api/v1/documents/{id}
Retrieves detailed information about a document, including extracted text and LLM analysis results.

**Request**:
No request body required.
```
GET /api/v1/documents/1
```

**Response**:
`200 OK`
```json
{
  "id": 1,
  "fileName": "example.pdf",
  "contentType": "application/pdf",
  "sizeBytes": 102400,
  "storagePath": "docxtractor-bucket/example-1700000000000-abc123.pdf",
  "extractedText": "This is the extracted text content...",
  "containsImages": true,
  "imageCount": 2,
  "analyzed": true,
  "documentType": "report",
  "summary": "This document summarizes the quarterly performance, detailing sales figures and market trends.",
  "metadataJson": "{\"names\":[\"John Doe\"],\"dates\":[\"2023-01-15\"],\"amounts\":[\"123.45\"],\"emails\":[\"john.doe@example.com\"],\"phones\":[]}"
}
```

**Errors**:
-   `400 Bad Request`: "Document not found: [id]"
-   `500 Internal Server Error`: "Internal error: [error message]"

## Contributing
We welcome contributions to the DocXtractor project. To contribute:

1.  Fork the repository.
2.  Create a new branch for your feature or bug fix.
3.  Implement your changes and ensure they adhere to the existing code style.
4.  Write comprehensive unit and integration tests.
5.  Ensure all tests pass.
6.  Submit a pull request with a clear description of your changes.

## License
This project is licensed under the MIT License. A formal `LICENSE` file is not explicitly included in the repository; however, contributions are assumed to be under this permissive open-source license.

## Author Info
*   **Name**: [Your Name Here]
*   **Email**: [Your Email Here]
*   **LinkedIn**: [Your LinkedIn Profile]
*   **Twitter**: [Your Twitter Handle]

## Badges
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-007396?style=for-the-badge&logo=java&logoColor=white)](https://www.java.com/)
[![MySQL](https://img.shields.io/badge/MySQL-4479A1?style=for-the-badge&logo=mysql&logoColor=white)](https://www.mysql.com/)
[![MinIO](https://img.shields.io/badge/MinIO-FF6600?style=for-the-badge&logo=minio&logoColor=white)](https://min.io/)
[![Tesseract OCR](https://img.shields.io/badge/Tesseract_OCR-4A4A4A?style=for-the-badge&logo=tesseract&logoColor=white)](https://tesseract-ocr.github.io/)

[![Readme was generated by Dokugen](https://img.shields.io/badge/Readme%20was%20generated%20by-Dokugen-brightgreen)](https://www.npmjs.com/package/dokugen)