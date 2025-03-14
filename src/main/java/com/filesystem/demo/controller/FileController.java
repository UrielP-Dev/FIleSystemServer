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
        // Extraemos el token y validamos al usuario (se puede omitir el filtrado de compañía si se desea que
        // cualquier token con autorización pueda acceder al archivo)
        String token = jwtService.extractToken(request);
        UserMetadata userMetadata = jwtService.extractUserMetadata(token);
        if (userMetadata == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        FileMetadata metadata = fileMetadataService.getFileMetadataById(id);
        if (metadata == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        // Nota: Si deseas restringir el acceso para que solo se puedan ver archivos de la misma compañía,
        // puedes incluir la validación de compañía aquí. En este ejemplo se asume que el endpoint es accesible
        // para cualquier token autorizado.

        Path filePath = Paths.get(metadata.getFilePath());
        try {
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

            String contentType = metadata.getContentType() != null
                    ? metadata.getContentType()
                    : "application/octet-stream";

            // Si el tipo de contenido es de imagen, se mostrará en el navegador (inline).
            // En otro caso, se forzará la descarga (attachment).
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
}
