package com.VetSphere.Backend.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Documentacion OpenAPI/Swagger de la API.
 * En produccion la UI y /v3/api-docs quedan protegidas por rol ADMIN
 * (ver {@link SecurityConfig}); en local se habilitan con app.swagger.enabled=true.
 */
@Configuration
public class OpenApiConfig {

    public static final String ESQUEMA_JWT = "bearerAuth";

    @Bean
    public OpenAPI vetSphereOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("VetSphere API")
                        .description("API REST de la clinica veterinaria VetSphere: autenticacion JWT, "
                                + "pacientes, facturacion, farmacia, citas y asistente inteligente.")
                        .version("2.0.0")
                        .contact(new Contact().name("Equipo VetSphere").email("soporte@vetsphere.com"))
                        .license(new License().name("Uso academico")))
                .addSecurityItem(new SecurityRequirement().addList(ESQUEMA_JWT))
                .components(new Components().addSecuritySchemes(ESQUEMA_JWT, new SecurityScheme()
                        .name(ESQUEMA_JWT)
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("Pega aqui el token que devuelve /api/auth/login")));
    }
}
