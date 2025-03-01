package com.filesystem.demo.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "fileMetadata")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileMetadata {

    @Id
    private String id;

    private String fileName;
    private String contentType;
    private long size;
    private LocalDateTime uploadDate;
    private String uploadedBy;
    private String company;
    private LocalDateTime updateDate;
}
