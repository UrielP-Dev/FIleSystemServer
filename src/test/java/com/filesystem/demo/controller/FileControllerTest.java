package com.filesystem.demo.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.filesystem.demo.model.FileMetadata;
import com.filesystem.demo.model.UserMetadata;
import com.filesystem.demo.service.FileMetadataService;
import com.filesystem.demo.service.FileStorageService;
import com.filesystem.demo.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@WebMvcTest(FileController.class)
class FileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FileStorageService fileStorageService;

    @MockBean
    private FileMetadataService fileMetadataService;

    @MockBean
    private JwtService jwtService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String token = "Bearer testToken";
    private final UserMetadata userMetadata = new UserMetadata();

    @BeforeEach
    void setUp() {
        userMetadata.setUserId("testUserId");
        userMetadata.setUsername("testUser");
        userMetadata.setCompany("testCompany");
        when(jwtService.extractUserMetadata("testToken")).thenReturn(userMetadata);
        when(jwtService.extractToken(any())).thenReturn("testToken");
    }

    @Test
    @WithMockUser
    void uploadFileShouldReturnSuccess() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", "test content".getBytes());
        Path filePath = Paths.get("test/path/test.txt");

        when(fileStorageService.storeFile(file)).thenReturn(filePath);

        mockMvc.perform(MockMvcRequestBuilders.multipart("/files/upload")
                        .file(file)
                        .header("Authorization", token))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(true))
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("File uploaded and metadata saved successfully"));
    }

    @Test
    @WithMockUser
    void uploadFileShouldReturnBadRequest() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", "test content".getBytes());
        when(fileStorageService.storeFile(file)).thenThrow(new IllegalArgumentException("Invalid file"));

        mockMvc.perform(MockMvcRequestBuilders.multipart("/files/upload")
                        .file(file)
                        .header("Authorization", token))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(false))
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("Invalid file"));
    }

    @Test
    @WithMockUser
    void getFilesShouldReturnFiles() throws Exception {
        Map<String, Object> response = Map.of("success", true, "data", Collections.emptyList());
        when(fileMetadataService.getFilesResponse(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(response);

        mockMvc.perform(MockMvcRequestBuilders.get("/files")
                        .header("Authorization", token))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser
    void downloadOrDisplayFileShouldReturnFileContent() throws Exception {
        String fileId = "testId";
        FileMetadata metadata = new FileMetadata();
        metadata.setFilePath("test/path/test.txt");
        metadata.setFileName("test.txt");
        metadata.setContentType("text/plain");

        when(fileMetadataService.getFileMetadataById(fileId)).thenReturn(metadata);

        mockMvc.perform(MockMvcRequestBuilders.get("/files/download/" + fileId)
                        .header("Authorization", token))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.header().string("Content-Disposition", "inline; filename=\"test.txt\""));
    }

    @Test
    @WithMockUser
    void updateFileMetadataShouldReturnSuccess() throws Exception {
        String fileId = "testId";
        FileMetadata updatedMetadata = new FileMetadata();
        updatedMetadata.setFileName("newTest.txt");

        when(fileMetadataService.updateFileMetadata(eq(fileId), eq(updatedMetadata), any())).thenReturn(true);

        mockMvc.perform(MockMvcRequestBuilders.put("/files/" + fileId)
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedMetadata)))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser
    void updateFileMetadataShouldReturnForbidden() throws Exception {
        String fileId = "testId";
        FileMetadata updatedMetadata = new FileMetadata();
        updatedMetadata.setFileName("newTest.txt");

        when(fileMetadataService.updateFileMetadata(eq(fileId), eq(updatedMetadata), any())).thenReturn(false);

        mockMvc.perform(MockMvcRequestBuilders.put("/files/" + fileId)
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedMetadata)))
                .andExpect(MockMvcResultMatchers.status().isForbidden())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(false));
    }

    @Test
    @WithMockUser
    void deleteFileShouldReturnSuccess() throws Exception {
        String fileId = "testId";
        when(fileMetadataService.deleteFile(fileId, userMetadata)).thenReturn(true);

        mockMvc.perform(MockMvcRequestBuilders.delete("/files/" + fileId)
                        .header("Authorization", token))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser
    void deleteFileShouldReturnForbidden() throws Exception {
        String fileId = "testId";
        when(fileMetadataService.deleteFile(fileId, userMetadata)).thenReturn(false);

        mockMvc.perform(MockMvcRequestBuilders.delete("/files/" + fileId)
                        .header("Authorization", token))
                .andExpect(MockMvcResultMatchers.status().isForbidden())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(false));
    }

    @Test
    @WithMockUser
    void getFileVersionsShouldReturnVersions() throws Exception {
        String fileId = "testId";
        when(fileMetadataService.getAllVersions(fileId)).thenReturn(Collections.emptyList());

        mockMvc.perform(MockMvcRequestBuilders.get("/files/versions/" + fileId)
                        .header("Authorization", token))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser
    void uploadFileVersionShouldReturnSuccess() throws Exception {
        String fileId = "testId";
        MockMultipartFile file = new MockMultipartFile("file", "newTest.txt", "text/plain", "new content".getBytes());

        mockMvc.perform(MockMvcRequestBuilders.multipart("/files/upload/version/" + fileId)
                        .file(file)
                        .header("Authorization", token))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(true));
    }
}
