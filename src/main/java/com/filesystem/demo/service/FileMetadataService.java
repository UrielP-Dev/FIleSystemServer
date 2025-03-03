package com.filesystem.demo.service;

import com.filesystem.demo.model.FileMetadata;
import com.filesystem.demo.repository.FileMetadataRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class FileMetadataService {

    @Autowired
    private FileMetadataRepository fileMetadataRepository;

    public FileMetadata saveMetadata(FileMetadata metadata) {
        return fileMetadataRepository.save(metadata);
    }
}
