package com.VetSphere.Backend.modules.clinic.controller;

import com.VetSphere.Backend.exception.ResourceNotFoundException;
import com.VetSphere.Backend.modules.clinic.domain.Proveedor;
import com.VetSphere.Backend.modules.clinic.dto.ProveedorRequest;
import com.VetSphere.Backend.modules.clinic.repository.ProveedorRepository;
import com.VetSphere.Backend.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * CRUD completo de proveedores reales (persistidos en base de datos).
 * Antes era un facade con datos de demostracion hardcodeados.
 */
@Tag(name = "Proveedores", description = "Gestion de proveedores de la clinica (solo ADMIN)")
@RestController
@RequestMapping("/api/proveedores")
@RequiredArgsConstructor
public class ProveedorController {
    private final ProveedorRepository proveedorRepository;

    @GetMapping
    @Operation(summary = "Lista los proveedores")
    public ApiResponse<List<Proveedor>> listar() {
        return ApiResponse.ok("Proveedores", proveedorRepository.findAllByOrderByNombreAsc());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtiene un proveedor por id")
    public ApiResponse<Proveedor> obtener(@PathVariable Long id) {
        return ApiResponse.ok("Proveedor", buscar(id));
    }

    @PostMapping
    @Operation(summary = "Crea un proveedor")
    public ApiResponse<Proveedor> crear(@Valid @RequestBody ProveedorRequest request) {
        return ApiResponse.ok("Proveedor creado", proveedorRepository.save(construir(null, request)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualiza un proveedor")
    public ApiResponse<Proveedor> actualizar(@PathVariable Long id, @Valid @RequestBody ProveedorRequest request) {
        Proveedor proveedor = buscar(id);
        Proveedor actualizado = construir(id, request);
        actualizado.setActivo(request.activo() == null ? proveedor.isActivo() : request.activo());
        return ApiResponse.ok("Proveedor actualizado", proveedorRepository.save(actualizado));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Elimina un proveedor")
    public ApiResponse<Void> eliminar(@PathVariable Long id) {
        proveedorRepository.deleteById(id);
        return ApiResponse.ok("Proveedor eliminado", null);
    }

    private Proveedor buscar(Long id) {
        return proveedorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Proveedor no encontrado"));
    }

    private Proveedor construir(Long id, ProveedorRequest request) {
        return Proveedor.builder()
                .id(id)
                .nombre(request.nombre())
                .contacto(request.contacto())
                .telefono(request.telefono())
                .email(request.email())
                .direccion(request.direccion())
                .activo(request.activo() == null || request.activo())
                .build();
    }
}
