package com.filesystem.demo.exception;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Clase para representar respuestas de error estandarizadas.
 */
@Data
@AllArgsConstructor              // Constructor con todos los argumentos
@NoArgsConstructor               // Constructor sin argumentos (necesario para algunas librerías y tests)
@Builder                         // Permite construir el objeto con patrón builder
public class ErrorResponseTest {

    // Código de estado HTTP (ej. 400, 404, 500)
    private int status;

    // Tipo de error (ej. "Bad Request", "Not Found", "Internal Server Error")
    private String error;

    // Mensaje detallado para el cliente
    private String message;

    // Fecha y hora del error
    private LocalDateTime timestamp;

    // Ruta del endpoint donde ocurrió el error
    private String path;
}
