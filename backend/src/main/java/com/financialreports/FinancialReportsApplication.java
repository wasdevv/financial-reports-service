package com.financialreports;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

// Sem UserDetailsService: o login é por email + bcrypt no AuthService, e o filtro JWT
// recarrega o usuário por id. Excluir o autoconfig evita o usuário "user" em memória.
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@ConfigurationPropertiesScan
@OpenAPIDefinition(
        info = @Info(title = "Financial Reports API", version = "1.0.0",
                description = "Financial reports with server-side risk scoring, approval workflow and audit trail"),
        security = @SecurityRequirement(name = "bearer"))
@SecurityScheme(name = "bearer", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
public class FinancialReportsApplication {

    public static void main(String[] args) {
        SpringApplication.run(FinancialReportsApplication.class, args);
    }
}
