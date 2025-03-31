# File System Service

This microservice handles file storage and retrieval.

## 🚀 Technologies
- Java 17
- Spring Boot
- MongoDB (for metadata)
- MinIO/S3 (for file storage)
- JWT (for authentication)

---------------------------------------------
Generete JWT POST: 
https://apigateway.nicepebble-44974112.eastus.azurecontainerapps.io/auth/login
{
  "username": "",
  "password": ""
}

## URL:
https://apigateway.nicepebble-44974112.eastus.azurecontainerapps.io

## 📂 Endpoints
| Method | Endpoint         | Description                |
|--------|-----------------|----------------------------|
| POST   | `/files/upload)`       | Uploads a file            |
| GET    | `/files/{id}` | Downloads a file          |
| DELETE | `/files/{id}`  | Deletes a file            |

## 📦 Installation
1. Clone the repository.
2. Configure MongoDB and MinIO.
3. Run: `mvn spring-boot:run`
