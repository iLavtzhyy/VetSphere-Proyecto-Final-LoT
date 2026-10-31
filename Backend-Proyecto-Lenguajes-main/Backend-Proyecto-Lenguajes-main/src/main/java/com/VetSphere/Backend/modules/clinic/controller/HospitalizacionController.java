package com.VetSphere.Backend.modules.clinic.controller;

import com.VetSphere.Backend.modules.clinic.domain.Hospitalizacion;
import com.VetSphere.Backend.modules.clinic.domain.Mascota;
import com.VetSphere.Backend.modules.clinic.repository.HospitalizacionRepository;
import com.VetSphere.Backend.modules.clinic.repository.MascotaRepository;
import com.VetSphere.Backend.shared.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/hospitalizaciones")
@RequiredArgsConstructor
public class HospitalizacionController {
    private final HospitalizacionRepository hospitalizacionRepository;
    private final MascotaRepository mascotaRepository;
    private final com.VetSphere.Backend.modules.clinic.service.TarifaService tarifaService;
    private final com.VetSphere.Backend.modules.clinic.service.CuentaHospitalizacionService cuentaService;

    @GetMapping 
    public ApiResponse<List<Hospitalizacion>> listar() { 
        return ApiResponse.ok("Hospitalizaciones", hospitalizacionRepository.findAll()); 
    }

    @GetMapping("/{id}/cuenta")
    public ApiResponse<Map<String, Object>> cuenta(@PathVariable Long id) {
        return ApiResponse.ok("Cuenta acumulada de hospitalizacion", cuentaService.obtener(id));
    }

    @PostMapping("/{id}/consumos")
    public ApiResponse<Map<String, Object>> agregarConsumo(@PathVariable Long id, @RequestBody Map<String, Object> datos) {
        return ApiResponse.ok("Consumo agregado a la cuenta", cuentaService.agregarConsumo(id, datos));
    }

    @PostMapping("/{id}/alta")
    public ApiResponse<com.VetSphere.Backend.modules.clinic.domain.Factura> darAlta(@PathVariable Long id) {
        return ApiResponse.ok("Alta registrada y cuenta facturada", cuentaService.darAlta(id));
    }

    @PostMapping 
    public ApiResponse<Hospitalizacion> crear(@RequestBody Hospitalizacion hospitalizacion) { 
        // El identificador lo asigna el servidor: un alta jamas sobrescribe otra hospitalizacion.
        hospitalizacion.setId(null);
        if (hospitalizacion.getPrecio() == null) {
            hospitalizacion.setPrecio(tarifaService.precioHospitalizacion());
        }
        if (hospitalizacion.getIngreso() == null) {
            hospitalizacion.setIngreso(LocalDateTime.now());
        }
        if (hospitalizacion.getMascota() != null && hospitalizacion.getMascota().getId() != null) {
            Mascota mascota = mascotaRepository.findById(hospitalizacion.getMascota().getId()).orElse(null);
            if (mascota != null) {
                hospitalizacion.setMascota(mascota);
            }
        }
        return ApiResponse.ok("Hospitalizacion registrada", hospitalizacionRepository.save(hospitalizacion)); 
    }

    @PutMapping("/{id}")
    public ApiResponse<Hospitalizacion> actualizar(@PathVariable Long id, @RequestBody Map<String, Object> datos) {
        Hospitalizacion hospitalizacion = hospitalizacionRepository.findById(id).orElseThrow();
        if (datos.containsKey("ingreso") && datos.get("ingreso") != null && !datos.get("ingreso").toString().isBlank()) hospitalizacion.setIngreso(LocalDateTime.parse(datos.get("ingreso").toString()));
        if (datos.containsKey("alta") && datos.get("alta") != null && !datos.get("alta").toString().isBlank()) hospitalizacion.setAlta(LocalDateTime.parse(datos.get("alta").toString()));
        if (datos.containsKey("jaula")) hospitalizacion.setJaula(texto(datos.get("jaula")));
        if (datos.containsKey("motivo")) hospitalizacion.setMotivo(texto(datos.get("motivo")));
        if (datos.containsKey("planCuidados")) hospitalizacion.setPlanCuidados(texto(datos.get("planCuidados")));
        if (datos.containsKey("estado")) hospitalizacion.setEstado(texto(datos.get("estado")));
        if (datos.containsKey("precio") && datos.get("precio") != null) hospitalizacion.setPrecio(new java.math.BigDecimal(datos.get("precio").toString()));
        return ApiResponse.ok("Hospitalizacion actualizada", hospitalizacionRepository.save(hospitalizacion));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> eliminar(@PathVariable Long id) {
        hospitalizacionRepository.deleteById(id);
        return ApiResponse.ok("Hospitalizacion eliminada", null);
    }

    private String texto(Object valor) {
        return valor == null ? null : valor.toString();
    }
}
