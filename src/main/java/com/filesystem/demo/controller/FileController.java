package com.filesystem.demo.controller;

import com.filesystem.demo.exception.ErrorResponse;
import com.filesystem.demo.model.FileMetadata;
import com.filesystem.demo.model.UserMetadata;
import com.filesystem.demo.service.FileMetadataService;
import com.filesystem.demo.service.FileStorageService;
import com.filesystem.demo.service.JwtService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "File Management", description = "Operations pertaining to file management")
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

    @Operation(
            summary = "Upload a file",
            description = "Uploads a file and saves its metadata. Requires authentication via a JWT token in the Authorization header."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "File uploaded successfully", content = @Content(schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "400", description = "Bad request, e.g., invalid file", content = @Content(schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized, invalid or missing token", content = @Content(schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error, e.g., file saving failed", content = @Content(schema = @Schema(implementation = Map.class)))
    })
    @SecurityRequirement(name = "BearerAuth")
    @PostMapping("/upload")
    public ResponseEntity<?> uploadFile(
            @Parameter(description = "The file to upload", required = true) @RequestParam("file") MultipartFile file,
            HttpServletRequest request
    ) {
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

    @Operation(
            summary = "Get files",
            description = "Retrieves a list of files based on optional filters. Requires authentication via a JWT token in the Authorization header."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Files retrieved successfully", content = @Content(schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized, invalid or missing token", content = @Content(schema = @Schema(implementation = Map.class)))
    })    @SecurityRequirement(name = "BearerAuth")

    @SecurityRequirement(name = "BearerAuth")
    @GetMapping
    public ResponseEntity<?> getFiles(
            HttpServletRequest request,
            @Parameter(description = "Filter by file ID") @RequestParam(required = false) String id,
            @Parameter(description = "Filter by user ID") @RequestParam(required = false) String userId,
            @Parameter(description = "Filter by username") @RequestParam(required = false) String username,
            @Parameter(description = "Filter by file name") @RequestParam(required = false) String fileName,
            @Parameter(description = "Filter by file type") @RequestParam(required = false) String fileType,
            @Parameter(description = "Filter by upload date from (yyyy-MM-dd)") @RequestParam(required = false) String dateFrom,
            @Parameter(description = "Filter by upload date to (yyyy-MM-dd)") @RequestParam(required = false) String dateTo,
            @Parameter(description = "Filter by minimum file size in bytes") @RequestParam(required = false) Long minSize,
            @Parameter(description = "Filter by maximum file size in bytes") @RequestParam(required = false) Long maxSize,
            @Parameter(description = "Field to sort by") @RequestParam(required = false) String sortBy,
            @Parameter(description = "Sort order (asc or desc)", example = "asc") @RequestParam(required = false, defaultValue = "asc") String order
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

        Map<String, Object> response = fileMetadataService.getFilesResponse(
                id,
                fileName,
                username,
                userId,
                company,
                fileType,
                dateFrom,
                dateTo,
                minSize,
                maxSize,
                sortBy,
                order
        );

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Download or display a file",
            description = "Downloads or displays a file based on its type. Images are displayed inline, other files are downloaded."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "File retrieved successfully", content = @Content(mediaType = "application/octet-stream")),
            @ApiResponse(responseCode = "404", description = "File not found", content = @Content(schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error, e.g., file processing failed", content = @Content(schema = @Schema(implementation = Map.class)))
    })    @SecurityRequirement(name = "BearerAuth")

    @GetMapping("/download/{id}")
    public ResponseEntity<?> downloadOrDisplayFile(
            @Parameter(description = "ID of the file to download or display", required = true) @PathVariable("id") String id
    ) {
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
            String contentType = (metadata.getContentType() != null)
                    ? metadata.getContentType() : "application/octet-stream";
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

    @Operation(
            summary = "Update file metadata",
            description = "Updates the metadata of a file. Requires authentication via a JWT token and sufficient permissions."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Metadata updated successfully", content = @Content(schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized, invalid or missing token", content = @Content(schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden, insufficient permissions", content = @Content(schema = @Schema(implementation = Map.class)))
    })
    @SecurityRequirement(name = "BearerAuth")
    @PutMapping("/{id}")
    public ResponseEntity<?> updateFileMetadata(
            @Parameter(description = "ID of the file to update", required = true) @PathVariable("id") String id,
            @Parameter(description = "Updated file metadata", required = true) @RequestBody FileMetadata updatedMetadata,
            HttpServletRequest request
    ) {
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

    @Operation(
            summary = "Delete a file",
            description = "Deletes a file. Requires authentication via a JWT token and sufficient permissions."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "File deleted successfully", content = @Content(schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized, invalid or missing token", content = @Content(schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden, insufficient permissions", content = @Content(schema = @Schema(implementation = Map.class)))
    })
    @SecurityRequirement(name = "BearerAuth")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteFile(
            @Parameter(description = "ID of the file to delete", required = true) @PathVariable("id") String id,
            HttpServletRequest request
    ) {
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

    @Operation(
            summary = "Get file versions",
            description = "Retrieves all versions of a file. Requires authentication via a JWT token."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Versions retrieved successfully", content = @Content(schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized, invalid or missing token", content = @Content(schema = @Schema(implementation = Map.class)))
    })
    @SecurityRequirement(name = "BearerAuth")
    @GetMapping("/versions/{fileId}")
    public ResponseEntity<?> getFileVersions(
            @Parameter(description = "ID of the file to retrieve versions for", required = true) @PathVariable("fileId") String fileId,
            HttpServletRequest request
    ) {
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

    @Operation(
            summary = "Upload a new file version",
            description = "Uploads a new version of an existing file. Requires authentication via a JWT token."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "New version uploaded successfully", content = @Content(schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized, invalid or missing token", content = @Content(schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error, e.g., file saving failed", content = @Content(schema = @Schema(implementation = Map.class)))
    })    @SecurityRequirement(name = "BearerAuth")
    @SecurityRequirement(name = "BearerAuth")
    @PostMapping("/upload/version/{fileId}")
    public ResponseEntity<?> uploadFileVersion(
            @Parameter(description = "ID of the file to upload a new version for", required = true) @PathVariable("fileId") String fileId,
            @Parameter(description = "The new file version to upload", required = true) @RequestParam("file") MultipartFile file,
            HttpServletRequest request
    ) {
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