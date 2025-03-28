package com.filesystem.demo.service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.MockitoAnnotations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.web.multipart.MultipartFile;

import com.filesystem.demo.model.FileMetadata;
import com.filesystem.demo.model.UserMetadata;
import com.filesystem.demo.repository.FileMetadataRepository;

class FileMetadataServiceTest {

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private FileMetadataRepository fileMetadataRepository;

    @Mock
    private MongoTemplate mongoTemplate;

    @Mock
    private MultipartFile file;

    @InjectMocks
    private FileMetadataService fileMetadataService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testSaveFileMetadata() {
        Path path = Path.of("/mock/path");
        when(file.getSize()).thenReturn(123L);
        when(file.getContentType()).thenReturn("text/plain");

        UserMetadata user = new UserMetadata();
        user.setUserId("user1");
        user.setUsername("testuser");
        user.setCompany("testcorp");
        user.setRole("admin");

        fileMetadataService.saveFileMetadata("test.txt", path, file, user);

        verify(fileMetadataRepository, times(1)).save(any(FileMetadata.class));
    }

    @Test
    void testGetFileMetadataById() {
        FileMetadata metadata = FileMetadata.builder().fileId("123").build();
        when(fileMetadataRepository.findById("123")).thenReturn(Optional.of(metadata));

        FileMetadata result = fileMetadataService.getFileMetadataById("123");
        assertNotNull(result);
        assertEquals("123", result.getFileId());
    }

    @Test
    void testUpdateFileMetadataWhenFileDoesNotExist() {
        UserMetadata user = new UserMetadata();
        user.setUserId("user1");

        FileMetadata updated = FileMetadata.builder().fileName("new.txt").contentType("text/plain").build();

        when(fileMetadataRepository.findById("missing")).thenReturn(Optional.empty());

        boolean result = fileMetadataService.updateFileMetadata("missing", updated, user);
        assertFalse(result);
    }

    @Test
    void testUpdateFileMetadataFailsIfNotOwner() {
        UserMetadata user = new UserMetadata();
        user.setUserId("user2");

        FileMetadata existing = FileMetadata.builder().uploaderId("user1").build();
        FileMetadata updated = FileMetadata.builder().fileName("new.txt").contentType("text/plain").build();

        when(fileMetadataRepository.findById("file1")).thenReturn(Optional.of(existing));

        boolean result = fileMetadataService.updateFileMetadata("file1", updated, user);
        assertFalse(result);
    }

    @Test
    void testDeleteFileFailsIfNotOwner() {
        UserMetadata user = new UserMetadata();
        user.setUserId("user2");

        FileMetadata file = FileMetadata.builder().uploaderId("user1").build();

        when(fileMetadataRepository.findById("file1")).thenReturn(Optional.of(file));

        boolean result = fileMetadataService.deleteFile("file1", user);
        assertFalse(result);
    }

    @Test
    void testDeleteFileSuccess() {
        UserMetadata user = new UserMetadata();
        user.setUserId("user1");

        FileMetadata file = FileMetadata.builder()
                .uploaderId("user1")
                .filePath("/mock/path")
                .build();

        when(fileMetadataRepository.findById("file1")).thenReturn(Optional.of(file));
        when(fileStorageService.deleteFile("/mock/path")).thenReturn(true);

        boolean result = fileMetadataService.deleteFile("file1", user);
        assertTrue(result);
        verify(fileMetadataRepository).deleteById("file1");
    }

    @Test
    void testUploadNewVersion() throws IOException {
        MultipartFile file = mock(MultipartFile.class);
        UserMetadata user = new UserMetadata();
        user.setUserId("user1");
        user.setUsername("testuser");
        user.setCompany("testcorp");
        user.setRole("admin");

        FileMetadata existing = FileMetadata.builder()
                .fileId("file1")
                .fileName("report.txt")
                .version(1)
                .build();

        Path newPath = Path.of("/mock/path/report_v2.txt");

        when(fileMetadataRepository.findTopByFileIdOrderByVersionDesc("file1"))
                .thenReturn(existing);
        when(file.getOriginalFilename()).thenReturn("report.txt");
        when(file.getSize()).thenReturn(456L);
        when(file.getContentType()).thenReturn("text/plain");
        when(fileStorageService.storeFile(file, "report.txt_v2")).thenReturn(newPath);

        fileMetadataService.uploadNewVersion("file1", file, user);

        verify(fileMetadataRepository).save(any(FileMetadata.class));
    }

    @Test
    void testGetAllVersions() {
        List<FileMetadata> versions = List.of(
                FileMetadata.builder().version(2).build(),
                FileMetadata.builder().version(1).build()
        );

        when(fileMetadataRepository.findByFileIdOrderByVersionDesc("file1")).thenReturn(versions);

        List<FileMetadata> result = fileMetadataService.getAllVersions("file1");
        assertEquals(2, result.size());
        assertEquals(2, result.get(0).getVersion());
    }
}
