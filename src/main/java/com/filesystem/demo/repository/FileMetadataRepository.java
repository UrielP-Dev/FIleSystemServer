package com.filesystem.demo.repository;

import com.filesystem.demo.model.FileMetadata;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FileMetadataRepository extends MongoRepository<FileMetadata, String> {
}
