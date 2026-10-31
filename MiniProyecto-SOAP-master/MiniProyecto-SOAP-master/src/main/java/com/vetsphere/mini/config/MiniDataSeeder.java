package com.vetsphere.mini.config;

import com.vetsphere.mini.domain.ProductoMini;
import com.vetsphere.mini.repository.ProductoMiniRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.List;

@Configuration
@Profile("local")
@RequiredArgsConstructor
public class MiniDataSeeder {
    private final ProductoMiniRepository repository;

    @Bean
    CommandLineRunner cargarCatalogoLocal() {
        return args -> {
            if (repository.count() > 0) return;

            List<ProductoMini> productos = List.of(
                    producto("MED-001", "Cefalexina 250mg"),
                    producto("MED-002", "Meloxicam unidad"),
                    producto("MED-003", "Atropina 10ml"),
                    producto("MED-004", "Solucion Hartmann 250mg"),
                    producto("MED-005", "Ketamina unidad"),
                    producto("MED-006", "Vacuna multiple canina 10ml"),
                    producto("MED-007", "Vacuna triple felina 250mg"),
                    producto("MED-008", "Omeprazol unidad"),
                    producto("MED-009", "Metronidazol 10ml"),
                    producto("MED-010", "Amoxicilina 250mg")
            );
            repository.saveAll(productos);
        };
    }

    private ProductoMini producto(String codigo, String nombre) {
        return ProductoMini.builder().codigo(codigo).nombre(nombre).stock(0).build();
    }
}
