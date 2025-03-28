package com.filesystem.demo.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import io.swagger.v3.oas.models.OpenAPI;

@SpringBootTest
class OpenApiConfigTest {

    @Autowired
    private OpenApiConfig openApiConfig;

    @Test
    void customOpenAPIShouldReturnConfiguredOpenAPI() {
        // Obtener el bean OpenAPI creado por la configuración
        OpenAPI openAPI = openApiConfig.customOpenAPI();

        // Verificar que el objeto OpenAPI no sea nulo
        assertNotNull(openAPI);

        // Verificar la información de la API
        assertNotNull(openAPI.getInfo());
        assertEquals("File System API", openAPI.getInfo().getTitle());
        assertEquals("1.0", openAPI.getInfo().getVersion());
        assertEquals("File Storage Microservice API Documentation", openAPI.getInfo().getDescription());

        // Verificar la configuración de seguridad (BearerAuth)
        assertNotNull(openAPI.getComponents());
        assertNotNull(openAPI.getComponents().getSecuritySchemes());
        assertNotNull(openAPI.getComponents().getSecuritySchemes().get("BearerAuth"));
        assertEquals("BearerAuth", openAPI.getComponents().getSecuritySchemes().get("BearerAuth").getName());
    }
}