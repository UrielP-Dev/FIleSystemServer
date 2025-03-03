package com.filesystem.demo.controller;

import com.filesystem.demo.model.FileMetadata;
import com.filesystem.demo.service.FileMetadataService;
import com.filesystem.demo.utils.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.file.*;
import java.util.Date;

@RestController
@RequestMapping("/files")
public class FileController {

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Autowired
    private FileMetadataService fileMetadataService;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/upload")
    public ResponseEntity<String> uploadFile(@RequestParam("file") MultipartFile file,
                                             HttpServletRequest request) {
        if (file.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("No file provided");
        }

        String fileName = StringUtils.cleanPath(file.getOriginalFilename());
        try {
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            Path filePath = uploadPath.resolve(fileName);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            String authHeader = request.getHeader("Authorization");
            String token = null;
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                token = authHeader.substring(7);
            }

            String uploaderId = token != null ? jwtUtil.extractUserId(token) : null;
            String uploaderUsername = token != null ? jwtUtil.extractUsername(token) : null;
            String uploaderCompany = token != null ? jwtUtil.extractCompany(token) : null;
            String uploaderRole = token != null ? jwtUtil.extractRole(token) : null;

            FileMetadata metadata = FileMetadata.builder()
                    .fileName(fileName)
                    .filePath(filePath.toAbsolutePath().toString())
                    .fileSize(file.getSize())
                    .contentType(file.getContentType())
                    .uploadDate(new Date())
                    .uploaderId(uploaderId)
                    .uploaderUsername(uploaderUsername)
                    .uploaderCompany(uploaderCompany)
                    .uploaderRole(uploaderRole)
                    .build();
            fileMetadataService.saveMetadata(metadata);

            return ResponseEntity.ok("File uploaded and metadata saved successfully: " + fileName);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error saving file: " + e.getMessage());
        }
    }
}
