package com.filesystem.demo.config;

import com.filesystem.demo.utils.JwtAuthenticationFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors; // Import adicional
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@WebMvcTest(SecurityConfig.class)
class SecurityConfigTest {

    @Autowired
    private SecurityFilterChain securityFilterChain;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void securityFilterChainShouldConfigureSecurityRules() throws Exception {
        // Verificar que el SecurityFilterChain no sea nulo
        assertNotNull(securityFilterChain);

        // Verificar el acceso permitido a "/files/download/**"
        mockMvc.perform(MockMvcRequestBuilders.get("/files/download/test.txt"))
                .andExpect(MockMvcResultMatchers.status().isOk());

        // Verificar el acceso permitido a "/swagger-ui/**"
        mockMvc.perform(MockMvcRequestBuilders.get("/swagger-ui/index.html"))
                .andExpect(MockMvcResultMatchers.status().isOk());

        // Verificar el acceso permitido a "/v3/api-docs/**"
        mockMvc.perform(MockMvcRequestBuilders.get("/v3/api-docs/openapi.yaml"))
                .andExpect(MockMvcResultMatchers.status().isOk());

        // Verificar que cualquier otra petición requiere autenticación (ejemplo: "/files")
        // Agregar un usuario autenticado ficticio (para un ejemplo de 401 más correcto)
        mockMvc.perform(MockMvcRequestBuilders.get("/files")
                        .with(SecurityMockMvcRequestPostProcessors.anonymous())  // o .user("test").password("password").withRoles("USER")
                )
                .andExpect(MockMvcResultMatchers.status().isUnauthorized()); // Correcto para un acceso no autenticado.


        // Si esperas un 403 (Forbidden), usarías:
        // mockMvc.perform(MockMvcRequestBuilders.get("/files"))
        //          .andExpect(MockMvcResultMatchers.status().isForbidden());
    }

    @Test
    void securityFilterChainShouldAddJwtFilter() {
        // Verificar que el filtro JWT esté configurado
        assertTrue(securityFilterChain.getFilters().contains(jwtAuthenticationFilter));
    }
}