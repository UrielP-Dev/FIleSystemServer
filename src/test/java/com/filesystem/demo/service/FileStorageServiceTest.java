package com.filesystem.demo.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class FileStorageServiceTest {

    private FileStorageService fileStorageService;
    private Path tempDir;

    @BeforeEach
    void setUp() throws IOException {
        tempDir = Files.createTempDirectory("uploads");
        fileStorageService = new FileStorageService();

        // Usar reflexión para establecer el valor de uploadDir
        try {
            var field = FileStorageService.class.getDeclaredField("uploadDir");
            field.setAccessible(true);
            field.set(fileStorageService, tempDir.toString());
        } catch (Exception e) {
            fail("No se pudo establecer uploadDir por reflexión: " + e.getMessage());
        }
    }

    @Test
    void testDeleteFileNonExistent() {
        Path filePath = tempDir.resolve("nonexistent.txt");
        boolean deleted = fileStorageService.deleteFile(filePath.toString());
        assertFalse(deleted);
    }

    @Test
    void testStoreEmptyFileThrowsException() {
        MockMultipartFile emptyFile = new MockMultipartFile("file", "empty.txt", "text/plain", new byte[0]);

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            fileStorageService.storeFile(emptyFile, "anyname.txt");
        });

        assertEquals("No file provided", exception.getMessage());
    }

    @Test
    void testStoreFileSuccessfully() throws IOException {
        byte[] content = "Hello, World!".getBytes();
        MockMultipartFile file = new MockMultipartFile("file", "hello.txt", "text/plain", content);

        Path storedPath = fileStorageService.storeFile(file);

        assertTrue(Files.exists(storedPath));
        assertEquals("hello.txt", storedPath.getFileName().toString());

        // Limpieza
        Files.deleteIfExists(storedPath);
    }

    @Test
    void testStoreFileWithCustomNameSuccessfully() throws IOException {
        byte[] content = "Custom Name".getBytes();
        MockMultipartFile file = new MockMultipartFile("file", "ignored.txt", "text/plain", content);

        String customName = "customName.txt";
        Path storedPath = fileStorageService.storeFile(file, customName);

        assertTrue(Files.exists(storedPath));
        assertEquals(customName, storedPath.getFileName().toString());

        // Limpieza
        Files.deleteIfExists(storedPath);
    }

    @Test
    void testStoreFileCreatesDirectoryIfNotExists() throws IOException {
        Files.deleteIfExists(tempDir); // Asegurarse que no existe

        byte[] content = "Auto Create Dir".getBytes();
        MockMultipartFile file = new MockMultipartFile("file", "file.txt", "text/plain", content);

        Path storedPath = fileStorageService.storeFile(file);

        assertTrue(Files.exists(storedPath));
        assertEquals("file.txt", storedPath.getFileName().toString());

        // Limpieza
        Files.deleteIfExists(storedPath);
    }
}