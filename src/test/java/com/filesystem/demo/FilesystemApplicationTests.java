package com.filesystem.demo;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class FilesystemApplicationTests {

    @Test
    void contextLoads() {
        // Esta prueba verifica si el contexto de Spring Boot se carga correctamente.
        // Si la aplicación se inicia sin excepciones, la prueba pasa.
        assertTrue(true); // Una afirmación básica para evitar advertencias de "prueba vacía"
    }

    @Test
    void applicationStarts() {
        // Esta prueba verifica que el método main de FilesystemApplication se ejecuta sin errores.
        // No hay lógica de retorno en el método main, por lo que simplemente verificamos que no lance excepciones.
        try {
            FilesystemApplication.main(new String[] {});
        } catch (Exception e) {
            // Si llega aquí, significa que el método main lanzó una excepción.
            // Para una prueba simple, podemos fallar si se lanza una excepción.
            assertTrue(false, "El método main lanzó una excepción: " + e.getMessage());
        }
        assertTrue(true); // Si no hubo excepciones, la prueba pasa.
    }
}