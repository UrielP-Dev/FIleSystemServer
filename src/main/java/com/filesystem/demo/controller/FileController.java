package com.filesystem.demo.controller;

import com.filesystem.demo.exception.ErrorResponse;
import com.filesystem.demo.model.FileMetadata;
import com.filesystem.demo.model.UserMetadata;
import com.filesystem.demo.service.FileMetadataService;
import com.filesystem.demo.service.FileStorageService;
import com.filesystem.demo.service.JwtService;
import lombok.extern.slf4j.Slf4j;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/files")
@Slf4j
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
    public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile file,
                                        HttpServletRequest request) {
        String token = jwtService.extractToken(request);
        UserMetadata userMetadata = jwtService.extractUserMetadata(token);
        if (userMetadata == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "success", false,
                    "message", "Unauthorized"
            ));
        }
        try {
            Path filePath = fileStorageService.storeFile(file);
            fileMetadataService.saveFileMetadata(file.getOriginalFilename(), filePath, file, userMetadata);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "File uploaded and metadata saved successfully",
                    "fileName", file.getOriginalFilename()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "message", "Error saving file: " + e.getMessage()
            ));
        }
    }

    @GetMapping
    public ResponseEntity<?> getFiles(
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
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "success", false,
                    "message", "Unauthorized"
            ));
        }
        String company = userMetadata.getCompany();
        List<FileMetadata> files = fileMetadataService.findFiles(userId, company, fileType, dateFrom, dateTo, minSize, maxSize, sortBy, order);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Files retrieved successfully",
                "data", files
        ));
    }

    @GetMapping("/download/{id}")
    public ResponseEntity<?> downloadOrDisplayFile(@PathVariable("id") String id, HttpServletRequest request) {
        String token = jwtService.extractToken(request);
        UserMetadata userMetadata = jwtService.extractUserMetadata(token);
        if (userMetadata == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "success", false,
                    "message", "Unauthorized"
            ));
        }
        FileMetadata metadata = fileMetadataService.getFileMetadataById(id);
        if (metadata == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "success", false,
                    "message", "File not found"
            ));
        }
        Path filePath = Paths.get(metadata.getFilePath());
        try {
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                        "success", false,
                        "message", "File not found or not readable"
                ));
            }
            String contentType = metadata.getContentType() != null ? metadata.getContentType() : "application/octet-stream";
            String contentDisposition = contentType.startsWith("image/") ?
                    "inline; filename=\"" + metadata.getFileName() + "\"" :
                    "attachment; filename=\"" + metadata.getFileName() + "\"";
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
                    .body(resource);
        } catch (MalformedURLException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "message", "Error processing file download"
            ));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateFileMetadata(@PathVariable("id") String id,
                                                @RequestBody FileMetadata updatedMetadata,
                                                HttpServletRequest request) {
        String token = jwtService.extractToken(request);
        UserMetadata userMetadata = jwtService.extractUserMetadata(token);
        if (userMetadata == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "success", false,
                    "message", "Unauthorized"
            ));
        }
        boolean updated = fileMetadataService.updateFileMetadata(id, updatedMetadata, userMetadata);
        if (updated) {
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "File metadata updated successfully"
            ));
        } else {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "success", false,
                    "message", "You are not allowed to update this file"
            ));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteFile(@PathVariable("id") String id, HttpServletRequest request) {
        String token = jwtService.extractToken(request);
        UserMetadata userMetadata = jwtService.extractUserMetadata(token);
        if (userMetadata == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "success", false,
                    "message", "Unauthorized"
            ));
        }
        boolean deleted = fileMetadataService.deleteFile(id, userMetadata);
        if (deleted) {
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "File deleted successfully"
            ));
        } else {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "success", false,
                    "message", "You are not allowed to delete this file"
            ));
        }
    }

    @GetMapping("/versions/{fileId}")
    public ResponseEntity<?> getFileVersions(@PathVariable("fileId") String fileId,
                                             HttpServletRequest request) {
        String token = jwtService.extractToken(request);
        UserMetadata userMetadata = jwtService.extractUserMetadata(token);
        if (userMetadata == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "success", false,
                    "message", "Unauthorized"
            ));
        }
        List<FileMetadata> versions = fileMetadataService.getAllVersions(fileId);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "File versions retrieved successfully",
                "data", versions
        ));
    }

    @PostMapping("/upload/version/{fileId}")
    public ResponseEntity<?> uploadFileVersion(@PathVariable("fileId") String fileId,
                                               @RequestParam("file") MultipartFile file,
                                               HttpServletRequest request) {
        String token = jwtService.extractToken(request);
        UserMetadata userMetadata = jwtService.extractUserMetadata(token);
        if (userMetadata == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "success", false,
                    "message", "Unauthorized"
            ));
        }
        try {
            fileMetadataService.uploadNewVersion(fileId, file, userMetadata);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "New version uploaded successfully",
                    "fileId", fileId
            ));
        } catch (IOException e) {
            return handleException(e, request.getRequestURI());
        }
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception ex, String path) {
        log.error("Exception: {}", ex.getMessage(), ex);
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Internal Server Error",
                ex.getMessage(),
                LocalDateTime.now(),
                path
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}
