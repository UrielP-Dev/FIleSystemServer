package com.filesystem.demo.controller;

import com.filesystem.demo.model.FileMetadata;
import com.filesystem.demo.model.UserMetadata;
import com.filesystem.demo.service.FileMetadataService;
import com.filesystem.demo.service.FileStorageService;
import com.filesystem.demo.service.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import jakarta.servlet.http.HttpServletRequest;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@RestController
@RequestMapping("/files")
public class FileController {

    private final FileStorageService fileStorageService;
    private final FileMetadataService fileMetadataService;
    private final JwtService jwtService;

    @Autowired
    public FileController(FileStorageService fileStorageService,
                          FileMetadataService fileMetadataService,
                          JwtService jwtService) {
        this.fileStorageService = fileStorageService;
        this.fileMetadataService = fileMetadataService;
        this.jwtService = jwtService;
    }

    @PostMapping("/upload")
    public ResponseEntity<String> uploadFile(@RequestParam("file") MultipartFile file,
                                             HttpServletRequest request) {
        try {
            Path filePath = fileStorageService.storeFile(file);
            String token = jwtService.extractToken(request);
            UserMetadata userMetadata = jwtService.extractUserMetadata(token);
            fileMetadataService.saveFileMetadata(file.getOriginalFilename(), filePath, file, userMetadata);
            return ResponseEntity.ok("File uploaded and metadata saved successfully: " + file.getOriginalFilename());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error saving file: " + e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<List<FileMetadata>> getFiles(
            HttpServletRequest request,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String fileType,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            @RequestParam(required = false) Long minSize,
            @RequestParam(required = false) Long maxSize,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false, defaultValue = "asc") String order
    ) {
        String token = jwtService.extractToken(request);
        UserMetadata userMetadata = jwtService.extractUserMetadata(token);
        if (userMetadata == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String company = userMetadata.getCompany();
        List<FileMetadata> files = fileMetadataService.findFiles(
                userId, company, fileType, dateFrom, dateTo, minSize, maxSize, sortBy, order);
        return ResponseEntity.ok(files);
    }

    @GetMapping("/download/{id}")
    public ResponseEntity<Resource> downloadOrDisplayFile(@PathVariable("id") String id, HttpServletRequest request) {
        String token = jwtService.extractToken(request);
        UserMetadata userMetadata = jwtService.extractUserMetadata(token);
        if (userMetadata == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        FileMetadata metadata = fileMetadataService.getFileMetadataById(id);
        if (metadata == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }



        Path filePath = Paths.get(metadata.getFilePath());
        try {
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

            String contentType = metadata.getContentType() != null
                    ? metadata.getContentType()
                    : "application/octet-stream";


            String contentDisposition;
            if (contentType.startsWith("image/")) {
                contentDisposition = "inline; filename=\"" + metadata.getFileName() + "\"";
            } else {
                contentDisposition = "attachment; filename=\"" + metadata.getFileName() + "\"";
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
                    .body(resource);
        } catch (MalformedURLException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<String> updateFileMetadata(@PathVariable("id") String id,
                                                     @RequestBody FileMetadata updatedMetadata,
                                                     HttpServletRequest request) {
        String token = jwtService.extractToken(request);
        UserMetadata userMetadata = jwtService.extractUserMetadata(token);

        if (userMetadata == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        }

        boolean updated = fileMetadataService.updateFileMetadata(id, updatedMetadata, userMetadata);
        if (updated) {
            return ResponseEntity.ok("File metadata updated successfully.");
        } else {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("You are not allowed to update this file.");
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteFile(@PathVariable("id") String id, HttpServletRequest request) {
        String token = jwtService.extractToken(request);
        UserMetadata userMetadata = jwtService.extractUserMetadata(token);

        if (userMetadata == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        }

        boolean deleted = fileMetadataService.deleteFile(id, userMetadata);
        if (deleted) {
            return ResponseEntity.ok("File deleted successfully.");
        } else {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("You are not allowed to delete this file.");
        }
    }
    @GetMapping("/versions/{fileId}")
    public ResponseEntity<List<FileMetadata>> getFileVersions(@PathVariable("fileId") String fileId,
                                                              HttpServletRequest request) {
        // Extraer el token y obtener la metadata del usuario autenticado
        String token = jwtService.extractToken(request);
        UserMetadata userMetadata = jwtService.extractUserMetadata(token);

        if (userMetadata == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // Recuperar todas las versiones del archivo usando el fileId
        List<FileMetadata> versions = fileMetadataService.getAllVersions(fileId);

        return ResponseEntity.ok(versions);
    }
    @PostMapping("/upload/version/{fileId}")
    public ResponseEntity<String> uploadFileVersion(@PathVariable("fileId") String fileId,
                                                    @RequestParam("file") MultipartFile file,
                                                    HttpServletRequest request) {
        String token = jwtService.extractToken(request);
        UserMetadata userMetadata = jwtService.extractUserMetadata(token);
        if (userMetadata == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        }

        try {
            fileMetadataService.uploadNewVersion(fileId, file, userMetadata);
            return ResponseEntity.ok("Nueva versión subida correctamente para el archivo con fileId: " + fileId);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al subir la nueva versión: " + e.getMessage());
        }
    }


}
