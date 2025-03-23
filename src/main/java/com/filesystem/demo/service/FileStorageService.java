package com.filesystem.demo.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;

import java.io.IOException;

@Service
public class FileStorageService {

    private final S3Client s3Client;
    private final String region;
    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    public FileStorageService(@Value("${aws.access-key}") String accessKey,
                              @Value("${aws.secret-key}") String secretKey,
                              @Value("${aws.region}") String region) {
        this.region = region;
        this.s3Client = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)))
                .build();
    }

    public String storeFile(MultipartFile file, String newFileName) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("No file provided");
        }

        // Subir el archivo a S3 sin usar ACLs
        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(bucketName)
                        .key(newFileName)
                        .contentDisposition("inline")
                        .contentType(file.getContentType())
                        .build(),
                RequestBody.fromBytes(file.getBytes())
        );

        return "https://" + bucketName + ".s3." + region + ".amazonaws.com/" + newFileName;
    }



    public String storeFile(MultipartFile file) throws IOException {
        return storeFile(file, file.getOriginalFilename());
    }

    public boolean deleteFile(String fileName) {
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileName)
                    .build());
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
