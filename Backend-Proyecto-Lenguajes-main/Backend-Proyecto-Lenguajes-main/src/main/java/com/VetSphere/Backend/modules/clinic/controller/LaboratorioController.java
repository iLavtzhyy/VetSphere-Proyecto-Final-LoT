package com.VetSphere.Backend.modules.clinic.controller;

import com.VetSphere.Backend.modules.clinic.domain.Mascota;
import com.VetSphere.Backend.modules.clinic.domain.OrdenLaboratorio;
import com.VetSphere.Backend.modules.clinic.repository.OrdenLaboratorioRepository;
import com.VetSphere.Backend.modules.clinic.repository.MascotaRepository;
import com.VetSphere.Backend.shared.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/laboratorio")
@RequiredArgsConstructor
public class LaboratorioController {
    private final OrdenLaboratorioRepository ordenRepository;
    private final MascotaRepository mascotaRepository;
    private final com.VetSphere.Backend.modules.clinic.service.TarifaService tarifaService;

    @GetMapping 
    public ApiResponse<List<OrdenLaboratorio>> listar() { 
        return ApiResponse.ok("Ordenes de laboratorio", ordenRepository.findAll()); 
    }

    @PostMapping 
    public ApiResponse<OrdenLaboratorio> crear(@RequestBody OrdenLaboratorio orden) { 
        // El identificador lo asigna el servidor: un alta jamas sobrescribe otra orden.
        orden.setId(null);
        if (orden.getPrecio() == null) {
            orden.setPrecio(tarifaService.precioLaboratorio());
        }
        if (orden.getFecha() == null) {
            orden.setFecha(LocalDateTime.now());
        }
        if (orden.getMascota() != null && orden.getMascota().getId() != null) {
            Mascota mascota = mascotaRepository.findById(orden.getMascota().getId()).orElse(null);
            if (mascota != null) {
                orden.setMascota(mascota);
            }
        }
        return ApiResponse.ok("Orden creada", ordenRepository.save(orden)); 
    }

    @PutMapping("/{id}")
    public ApiResponse<OrdenLaboratorio> actualizar(@PathVariable Long id, @RequestBody Map<String, Object> datos) {
        OrdenLaboratorio orden = ordenRepository.findById(id).orElseThrow();
        if (datos.containsKey("fecha") && datos.get("fecha") != null && !datos.get("fecha").toString().isBlank()) orden.setFecha(LocalDateTime.parse(datos.get("fecha").toString()));
        if (datos.containsKey("tipoPrueba")) orden.setTipoPrueba(texto(datos.get("tipoPrueba")));
        if (datos.containsKey("resultado")) orden.setResultado(texto(datos.get("resultado")));
        if (datos.containsKey("estado")) orden.setEstado(texto(datos.get("estado")));
        if (datos.containsKey("archivoResultadoUrl")) orden.setArchivoResultadoUrl(texto(datos.get("archivoResultadoUrl")));
        if (datos.containsKey("precio") && datos.get("precio") != null) orden.setPrecio(new java.math.BigDecimal(datos.get("precio").toString()));
        return ApiResponse.ok("Orden actualizada", ordenRepository.save(orden));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> eliminar(@PathVariable Long id) {
        ordenRepository.deleteById(id);
        return ApiResponse.ok("Orden eliminada", null);
    }

    private String texto(Object valor) {
        return valor == null ? null : valor.toString();
    }
}
