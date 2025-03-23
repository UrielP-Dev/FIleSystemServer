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
import java.util.*;

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

    /**
     * Guarda la metadata del archivo utilizando la URL devuelta por S3.
     */
    public void saveFileMetadata(String fileName, String fileUrl, MultipartFile file, UserMetadata userMetadata) {
        String fileId = UUID.randomUUID().toString();

        FileMetadata metadata = FileMetadata.builder()
                .fileId(fileId)
                .fileName(fileName)
                .filePath(fileUrl)
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

    /**
     * Sube el archivo a S3, guarda la metadata y retorna la URL.
     */
    public String storeFile(MultipartFile file, UserMetadata userMetadata) throws IOException {
        String fileUrl = fileStorageService.storeFile(file);
        saveFileMetadata(file.getOriginalFilename(), fileUrl, file, userMetadata);
        return fileUrl;
    }

    /**
     * Elimina el archivo de S3 y borra la metadata en MongoDB.
     * Se utiliza el nombre de archivo (key en S3) para la eliminación.
     */
    public boolean deleteFile(String fileId, UserMetadata userMetadata) {
        FileMetadata fileMetadata = fileMetadataRepository.findById(fileId).orElse(null);
        if (fileMetadata == null || !fileMetadata.getUploaderId().equals(userMetadata.getUserId())) {
            return false;
        }

        boolean fileDeleted = fileStorageService.deleteFile(fileMetadata.getFileName());
        if (fileDeleted) {
            fileMetadataRepository.deleteById(fileId);
            return true;
        }
        return false;
    }

    /**
     * Sube una nueva versión de un archivo.
     */
    public void uploadNewVersion(String fileId, MultipartFile file, UserMetadata userMetadata) throws IOException {
        FileMetadata lastVersion = fileMetadataRepository.findTopByFileIdOrderByVersionDesc(fileId);
        int newVersion = (lastVersion != null) ? lastVersion.getVersion() + 1 : 1;

        String baseFileName = (lastVersion != null) ? lastVersion.getFileName() : file.getOriginalFilename();
        String newFileName = baseFileName + "_v" + newVersion;

        String fileUrl = fileStorageService.storeFile(file, newFileName);

        FileMetadata newMetadata = FileMetadata.builder()
                .fileId(fileId)
                .fileName(baseFileName)
                .filePath(fileUrl)
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

    /**
     * Recupera archivos aplicando filtros y ordenamientos.
     */
    public Map<String, Object> getFilesResponse(
            String id,
            String fileName,
            String username,
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

        if (id != null && !id.isEmpty()) {
            query.addCriteria(Criteria.where("_id").is(id));
        }
        if (fileName != null && !fileName.isEmpty()) {
            query.addCriteria(Criteria.where("fileName").regex(fileName, "i"));
        }
        if (username != null && !username.isEmpty()) {
            query.addCriteria(Criteria.where("uploaderUsername").regex(username, "i"));
        }
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
                // Manejo opcional de la excepción
            }
        }
        if (dateTo != null && !dateTo.isEmpty()) {
            try {
                Date toDate = dateFormat.parse(dateTo);
                query.addCriteria(Criteria.where("uploadDate").lte(toDate));
            } catch (ParseException e) {
                // Manejo opcional de la excepción
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

        List<FileMetadata> files = mongoTemplate.find(query, FileMetadata.class);
        String baseUrl = "http://localhost:8090/files/download/";
        List<Map<String, Object>> fileResponses = new ArrayList<>();
        for (FileMetadata file : files) {
            Map<String, Object> fileMap = new HashMap<>();
            fileMap.put("id", file.getId());
            fileMap.put("fileId", file.getFileId());
            fileMap.put("fileName", file.getFileName());
            fileMap.put("fileSize", file.getFileSize());
            fileMap.put("contentType", file.getContentType());
            fileMap.put("uploadDate", file.getUploadDate());
            fileMap.put("uploaderUsername", file.getUploaderUsername());
            fileMap.put("downloadUrl", baseUrl + file.getId());
            fileResponses.add(fileMap);
        }
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Files retrieved successfully");
        response.put("data", fileResponses);
        return response;
    }

    /**
     * Recupera la metadata de un archivo por su ID.
     */
    public FileMetadata getFileMetadataById(String id) {
        return fileMetadataRepository.findById(id).orElse(null);
    }

    /**
     * Actualiza la metadata de un archivo, verificando permisos.
     */
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
    public List<FileMetadata> getAllVersions(String fileId) {
        return fileMetadataRepository.findByFileIdOrderByVersionDesc(fileId);
    }
}
