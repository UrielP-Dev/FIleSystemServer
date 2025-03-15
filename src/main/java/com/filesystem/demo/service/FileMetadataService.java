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

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.nio.file.Path;
import java.util.UUID;

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
        // Generar un identificador único para el fileId
        String fileId = UUID.randomUUID().toString();

        FileMetadata metadata = FileMetadata.builder()
                .fileId(fileId)
                .fileName(fileName)
                .filePath(filePath.toAbsolutePath().toString())
                .fileSize(file.getSize())
                .contentType(file.getContentType())
                .uploadDate(new Date())
                .uploaderId(userMetadata != null ? userMetadata.getUserId() : null)
                .uploaderUsername(userMetadata != null ? userMetadata.getUsername() : null)
                .uploaderCompany(userMetadata != null ? userMetadata.getCompany() : null)
                .uploaderRole(userMetadata != null ? userMetadata.getRole() : null)
                .version(0)
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
            }
        }
        if (dateTo != null && !dateTo.isEmpty()) {
            try {
                Date toDate = dateFormat.parse(dateTo);
                query.addCriteria(Criteria.where("uploadDate").lte(toDate));
            } catch (ParseException e) {
            }
        }
        if (minSize != null) {
            query.addCriteria(Criteria.where("fileSize").gte(minSize));
        }
        if (maxSize != null) {
            query.addCriteria(Criteria.where("fileSize").lte(maxSize));
        }

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

    public void uploadNewVersion(String fileId, MultipartFile file, UserMetadata userMetadata) throws IOException {
        // Buscar la última versión del archivo usando el fileId.
        FileMetadata lastVersion = fileMetadataRepository.findTopByFileIdOrderByVersionDesc(fileId);
        int newVersion = (lastVersion != null) ? lastVersion.getVersion() + 1 : 1;

        // Definir el nombre del nuevo archivo (manteniendo el nombre base o agregando el número de versión).
        String baseFileName = (lastVersion != null) ? lastVersion.getFileName() : file.getOriginalFilename();
        String newFileName = baseFileName + "_v" + newVersion;

        // Almacenar el archivo con un nombre personalizado.
        Path filePath = fileStorageService.storeFile(file, newFileName);

        // Crear la metadata para la nueva versión.
        FileMetadata newMetadata = FileMetadata.builder()
                .fileId(fileId) // Se mantiene el mismo fileId para todas las versiones.
                .fileName(baseFileName) // Se conserva el nombre base.
                .filePath(filePath.toAbsolutePath().toString())
                .fileSize(file.getSize())
                .contentType(file.getContentType())
                .uploadDate(new Date())
                .uploaderId(userMetadata.getUserId())
                .uploaderUsername(userMetadata.getUsername())
                .uploaderCompany(userMetadata.getCompany())
                .uploaderRole(userMetadata.getRole())
                .version(newVersion)
                .build();

        fileMetadataRepository.save(newMetadata);
    }

    public List<FileMetadata> getAllVersions(String fileId) {
        return fileMetadataRepository.findByFileIdOrderByVersionDesc(fileId);
    }


}
