package com.filesystem.demo.service;

import com.filesystem.demo.model.FileMetadata;
import com.filesystem.demo.model.UserMetadata;
import com.filesystem.demo.repository.FileMetadataRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.nio.file.Path;

@Service
public class FileMetadataService {

    private final FileStorageService fileStorageService;
    private final FileMetadataRepository fileMetadataRepository;
    private final MongoTemplate mongoTemplate;

    @Autowired
    public FileMetadataService(FileStorageService fileStorageService,
                               FileMetadataRepository fileMetadataRepository,
                               MongoTemplate mongoTemplate) {
        this.fileStorageService = fileStorageService;
        this.fileMetadataRepository = fileMetadataRepository;
        this.mongoTemplate = mongoTemplate;
    }



    public void saveFileMetadata(String fileName, Path filePath, MultipartFile file, UserMetadata userMetadata) {
        FileMetadata metadata = FileMetadata.builder()
                .fileName(fileName)
                .filePath(filePath.toAbsolutePath().toString())
                .fileSize(file.getSize())
                .contentType(file.getContentType())
                .uploadDate(new Date())
                .uploaderId(userMetadata != null ? userMetadata.getUserId() : null)
                .uploaderUsername(userMetadata != null ? userMetadata.getUsername() : null)
                .uploaderCompany(userMetadata != null ? userMetadata.getCompany() : null)
                .uploaderRole(userMetadata != null ? userMetadata.getRole() : null)
                .build();

        fileMetadataRepository.save(metadata);
    }

    public List<FileMetadata> findFiles(
            String userId,
            String company,
            String fileType,
            String dateFrom,
            String dateTo,
            Long minSize,
            Long maxSize,
            String sortBy,
            String order
    ) {
        Query query = new Query();

        if (userId != null && !userId.isEmpty()) {
            query.addCriteria(Criteria.where("uploaderId").is(userId));
        }
        if (company != null && !company.isEmpty()) {
            query.addCriteria(Criteria.where("uploaderCompany").is(company));
        }
        if (fileType != null && !fileType.isEmpty()) {
            query.addCriteria(Criteria.where("contentType").is(fileType));
        }

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        if (dateFrom != null && !dateFrom.isEmpty()) {
            try {
                Date fromDate = dateFormat.parse(dateFrom);
                query.addCriteria(Criteria.where("uploadDate").gte(fromDate));
            } catch (ParseException e) {
                // Manejar la excepción o registrar el error según convenga
            }
        }
        if (dateTo != null && !dateTo.isEmpty()) {
            try {
                Date toDate = dateFormat.parse(dateTo);
                query.addCriteria(Criteria.where("uploadDate").lte(toDate));
            } catch (ParseException e) {
                // Manejar la excepción o registrar el error según convenga
            }
        }
        if (minSize != null) {
            query.addCriteria(Criteria.where("fileSize").gte(minSize));
        }
        if (maxSize != null) {
            query.addCriteria(Criteria.where("fileSize").lte(maxSize));
        }

        // Ordenación: sortBy puede ser "date" o "size"
        if (sortBy != null && !sortBy.isEmpty()) {
            Sort.Direction direction = ("desc".equalsIgnoreCase(order)) ? Sort.Direction.DESC : Sort.Direction.ASC;
            if ("date".equalsIgnoreCase(sortBy)) {
                query.with(Sort.by(direction, "uploadDate"));
            } else if ("size".equalsIgnoreCase(sortBy)) {
                query.with(Sort.by(direction, "fileSize"));
            }
        }

        return mongoTemplate.find(query, FileMetadata.class);
    }

    public FileMetadata getFileMetadataById(String id) {
        return fileMetadataRepository.findById(id).orElse(null);
    }
    public boolean updateFileMetadata(String fileId, FileMetadata updatedMetadata, UserMetadata userMetadata) {
        FileMetadata existingFile = fileMetadataRepository.findById(fileId).orElse(null);
        if (existingFile == null || !existingFile.getUploaderId().equals(userMetadata.getUserId())) {
            return false;
        }

        existingFile.setFileName(updatedMetadata.getFileName());
        existingFile.setContentType(updatedMetadata.getContentType());

        fileMetadataRepository.save(existingFile);
        return true;
    }

    public boolean deleteFile(String fileId, UserMetadata userMetadata) {
        FileMetadata fileMetadata = fileMetadataRepository.findById(fileId).orElse(null);
        if (fileMetadata == null || !fileMetadata.getUploaderId().equals(userMetadata.getUserId())) {
            return false;
        }

        boolean fileDeleted = fileStorageService.deleteFile(fileMetadata.getFilePath());
        if (fileDeleted) {
            fileMetadataRepository.deleteById(fileId);
            return true;
        }

        return false;
    }

}
