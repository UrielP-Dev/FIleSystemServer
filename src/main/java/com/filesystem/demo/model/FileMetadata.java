package com.filesystem.demo.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "file_metadata")
public class FileMetadata {
    @Id
    private String id;
    private String fileName;
    private String filePath;
    private long fileSize;
    private String contentType;
    private Date uploadDate;
    private String uploaderId;
    private String uploaderUsername;
    private String uploaderCompany;
    private String uploaderRole;
}
