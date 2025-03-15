package com.filesystem.demo.repository;

import com.filesystem.demo.model.FileMetadata;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FileMetadataRepository extends MongoRepository<FileMetadata, String> {
    FileMetadata findTopByFileIdOrderByVersionDesc(String fileId);
    List<FileMetadata> findByFileIdOrderByVersionDesc(String fileId);
}