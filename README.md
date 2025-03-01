# File System Service

This microservice handles file storage and retrieval.

## 🚀 Technologies
- Java 17
- Spring Boot
- MongoDB (for metadata)
- MinIO/S3 (for file storage)
- JWT (for authentication)

## 📂 Endpoints
| Method | Endpoint         | Description                |
|--------|-----------------|----------------------------|
| POST   | `/upload`       | Uploads a file            |
| GET    | `/download/{id}` | Downloads a file          |
| DELETE | `/delete/{id}`  | Deletes a file            |

## 📦 Installation
1. Clone the repository.
2. Configure MongoDB and MinIO.
3. Run: `mvn spring-boot:run`
