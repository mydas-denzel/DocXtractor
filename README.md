# 🌟 DocXtractor API

## Overview
DocXtractor is a robust Spring Boot backend service designed for intelligent document processing. It facilitates the uploading, storage, text extraction, and advanced analysis of various document types using Optical Character Recognition (OCR) and Large Language Models (LLMs). Built with Java 21, it leverages PostgreSQL for persistent storage, MinIO for scalable object storage, and integrates with OpenRouter for cutting-edge LLM capabilities.

## Features
-   💾 **File Upload & Storage**: Securely upload documents (PDF, DOCX, images) to a configured object storage (MinIO).
-   📝 **Text Extraction**: Automatically extract textual content from uploaded PDFs, DOCX files, and other text-based documents using Apache Tika, Apache POI, and PDFBox.
-   👁️ **Optical Character Recognition (OCR)**: Utilize Tesseract OCR to accurately extract text from image-based documents or image sections within PDFs.
-   🧠 **LLM-Powered Document Analysis**: Integrate with Large Language Models (via OpenRouter) to generate concise summaries, classify document types (e.g., invoice, CV, report), and extract structured entities (names, dates, amounts, emails, phone numbers).
-   📊 **Structured Metadata Extraction**: Store extracted text, image presence, document type, summary, and structured metadata persistently in a PostgreSQL database.
-   🛡️ **Robust Error Handling**: Comprehensive exception handling for file uploads, processing, and external API calls.
-   🔗 **API-First Design**: A clean and well-defined RESTful API for seamless integration with frontend applications.

## Getting Started

### Installation
To get DocXtractor up and running on your local machine, follow these steps:

1.  **Prerequisites**:
    *   ☕ Java Development Kit (JDK) 21 or later.
    *   📦 Apache Maven 3.6.0 or later.
    *   🐳 Docker (optional, for running local PostgreSQL and MinIO instances).
    *   📖 Tesseract OCR installed on your system, with `tessdata` configured (e.g., in `/usr/share/tessdata`).

2.  **Clone the Repository**:
    ```bash
    git clone https://github.com/mydas-denzel/DocXtractor.git
    cd DocXtractor
    ```

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
The application relies on several environment variables for configuration. These should be set in your `application.properties` file or as system environment variables.

| Variable Name              | Example Value                                       | Description                                                                 |
| :------------------------- | :-------------------------------------------------- | :-------------------------------------------------------------------------- |
| `POSTGRES_USERNAME`        | `admin`                                             | Username for PostgreSQL database connection.                                |
| `POSTGRES_PASSWORD`        | `password`                                          | Password for PostgreSQL database connection.                                |
| `MINIO_CONSOLE_URL`        | `http://localhost:9000`                             | Endpoint URL for the MinIO storage server.                                  |
| `MINIO_USERNAME`           | `minioadmin`                                        | Access key for MinIO storage.                                               |
| `MINIO_PASSWORD`           | `minioadmin`                                        | Secret key for MinIO storage.                                               |
| `OPENROUTER_API_KEY`       | `sk-xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx`               | API key for OpenRouter (or compatible LLM) service.                         |
| `OPENROUTER_MODEL`         | `openai/gpt-4o-mini`                                | The specific LLM model to use via OpenRouter.                               |
| `ocr.tessdata-path`        | `/usr/share/tessdata`                               | Path to the Tesseract tessdata directory.                                   |
| `ocr.language`             | `eng`                                               | OCR language code (e.g., `eng` for English).                                |

## API Documentation

### Base URL
`http://localhost:8080/api/v1/documents`

### Endpoints

#### POST /api/v1/documents/upload
Uploads a document for initial processing and text extraction.

**Request**:
This endpoint expects a `multipart/form-data` request with a field named `file` containing the document.

```http
POST /api/v1/documents/upload HTTP/1.1
Host: localhost:8080
Content-Type: multipart/form-data; boundary=---011000010111000001101001

---011000010111000001101001
Content-Disposition: form-data; name="file"; filename="my_document.pdf"
Content-Type: application/pdf

[Binary content of my_document.pdf]
---011000010111000001101001--
```

**Response**:
Returns a `DocumentUploadResponse` containing basic information about the uploaded document and a message indicating next steps.

```json
{
  "id": 123,
  "fileName": "my_document.pdf",
  "contentType": "application/pdf",
  "sizeBytes": 102400,
  "containsImages": true,
  "imageCount": 5,
  "message": "Uploaded and text extracted; call /documents/{id}/analyze to run LLM"
}
```

**Errors**:
-   `400 Bad Request`: No file provided, or other client-side validation errors.
-   `413 Payload Too Large`: File exceeds the configured maximum upload size.
-   `500 Internal Server Error`: An unexpected server error occurred during upload or initial text extraction.

#### POST /api/v1/documents/{id}/analyze
Triggers the LLM-powered analysis for a previously uploaded document.

**Request**:
No request body is required. The document ID is provided as a path variable.

**Response**:
Returns a `DocumentUploadResponse` with an updated message indicating the completion of LLM analysis.

```json
{
  "id": 123,
  "fileName": "my_document.pdf",
  "contentType": "application/pdf",
  "sizeBytes": 102400,
  "containsImages": true,
  "imageCount": 5,
  "message": "LLM analysis complete"
}
```

**Errors**:
-   `400 Bad Request`: Document with the specified `id` was not found.
-   `500 Internal Server Error`: An unexpected server error occurred during LLM processing.

#### GET /api/v1/documents/{id}
Retrieves detailed information and analysis results for a specific document.

**Request**:
No request body is required. The document ID is provided as a path variable.

**Response**:
Returns a `DocumentDetailsDto` containing all stored information about the document, including extracted text, LLM analysis results, and metadata.

```json
{
  "id": 123,
  "fileName": "my_document.pdf",
  "contentType": "application/pdf",
  "sizeBytes": 102400,
  "storagePath": "zeabur/my_document-1701234567890-abc123.pdf",
  "extractedText": "This is the extracted textual content from the document...",
  "containsImages": true,
  "imageCount": 5,
  "analyzed": true,
  "documentType": "report",
  "summary": "A concise summary generated by the LLM, describing the main points of the document.",
  "metadataJson": "{\"names\":[\"John Doe\",\"Jane Smith\"],\"dates\":[\"2023-01-01\",\"2023-12-31\"],\"amounts\":[\"$1,200.50\",\"€500.00\"],\"emails\":[\"john.doe@example.com\"],\"phones\":[\"+15551234567\"]}"
}
```

**Errors**:
-   `400 Bad Request`: Document with the specified `id` was not found.
-   `500 Internal Server Error`: An unexpected server error occurred.

## Usage
Here are some `curl` examples to demonstrate how to interact with the DocXtractor API:

1.  **Upload a Document**:
    ```bash
    curl -X POST \
      http://localhost:8080/api/v1/documents/upload \
      -H 'Content-Type: multipart/form-data' \
      -F 'file=@/path/to/your/document.pdf'
    ```
    *Replace `/path/to/your/document.pdf` with the actual path to your file.*

2.  **Analyze a Document**:
    After uploading, note the `id` from the response. Use it to trigger analysis.
    ```bash
    curl -X POST \
      http://localhost:8080/api/v1/documents/123/analyze
    ```
    *Replace `123` with the actual document ID.*

3.  **Get Document Details**:
    After analysis, retrieve the full details, including LLM results.
    ```bash
    curl -X GET \
      http://localhost:8080/api/v1/documents/123
    ```
    *Replace `123` with the actual document ID.*

## Technologies Used

| Technology           | Version  | Description                                                         |
| :------------------- | :------- | :------------------------------------------------------------------ |
| **Spring Boot**      | 4.0.0    | Framework for building robust, stand-alone, production-grade Spring applications. |
| **Java**             | 21       | The core programming language.                                      |
| **PostgreSQL**       | -        | Relational database for persistent storage of document metadata.    |
| **MinIO**            | 8.5.2    | High-performance, S3 compatible object storage.                     |
| **Tesseract OCR**    | 5.4.0    | Open-source OCR engine for extracting text from images.             |
| **Apache Tika**      | 2.9.1    | Toolkit for detecting and extracting metadata and text from files.  |
| **Apache POI**       | 5.2.5    | Java API for Microsoft Documents (used for DOCX).                   |
| **PDFBox**           | 2.0.30   | Open-source Java tool for working with PDF documents.               |
| **Lombok**           | -        | Library to reduce boilerplate code (getters, setters, constructors).|
| **Springdoc OpenAPI**| 2.5.0    | Automated API documentation using OpenAPI 3 and Swagger UI.         |
| **OpenRouter.ai**    | -        | Unified API for various Large Language Models.                      |

## Contributing
We welcome contributions to DocXtractor! If you'd like to contribute, please follow these guidelines:

1.  **Fork the repository** to your GitHub account.
2.  **Create a new branch** for your feature or bug fix: `git checkout -b feature/your-feature-name`.
3.  **Make your changes**, ensuring they adhere to the project's coding standards.
4.  **Commit your changes** with clear and concise messages.
5.  **Push your branch** to your forked repository.
6.  **Open a Pull Request** against the `main` branch of this repository, describing your changes in detail.

## Author Info
**HNG**

Connect with me:
*   [LinkedIn](https://linkedin.com/in/yourprofile)
*   [Twitter](https://twitter.com/yourprofile)
*   [Portfolio](https://yourportfolio.com)

---
[![Build Status](https://img.shields.io/badge/Build-Passing-brightgreen)](https://github.com/mydas-denzel/DocXtractor/actions)
[![Java Version](https://img.shields.io/badge/Java-21-red)](https://www.oracle.com/java/technologies/downloads/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.0.0-green)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/License-MIT-blue)](https://opensource.org/licenses/MIT)
[![Readme was generated by Dokugen](https://img.shields.io/badge/Readme%20was%20generated%20by-Dokugen-brightgreen)](https://www.npmjs.com/package/dokugen)