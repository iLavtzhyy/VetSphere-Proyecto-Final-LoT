package com.VetSphere.Backend.modules.clinic.service;

import com.VetSphere.Backend.modules.clinic.domain.HistorialClinico;
import com.VetSphere.Backend.modules.clinic.domain.Mascota;
import com.VetSphere.Backend.modules.clinic.repository.HistorialClinicoRepository;
import com.VetSphere.Backend.modules.clinic.repository.MascotaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Logica de consultas clinicas (historial). La tarifa por tipo de evento
 * ahora proviene de {@link TarifaService} en lugar de valores hardcodeados.
 */
@Service
@RequiredArgsConstructor
public class ConsultaService {
    private final HistorialClinicoRepository historialRepository;
    private final MascotaRepository mascotaRepository;
    private final TarifaService tarifaService;

    public List<Map<String, Object>> listar() {
        return mapear(historialRepository.findAll());
    }

    public List<Map<String, Object>> mapear(List<HistorialClinico> consultas) {
        return consultas.stream().map(this::mapear).toList();
    }

    private Map<String, Object> mapear(HistorialClinico consulta) {
        var mascota = consulta.getMascota();
        var propietario = mascota != null ? mascota.getPropietario() : null;
        var veterinario = consulta.getVeterinario();
        BigDecimal precio = consulta.getPrecio() != null ? consulta.getPrecio()
                : tarifaService.precioPorTipo(consulta.getTipoEvento());
        Map<String, Object> respuesta = new LinkedHashMap<>();
        respuesta.put("id", consulta.getId());
        respuesta.put("mascota", mascota != null ? mascota.getNombre() : "Sin mascota");
        respuesta.put("cliente", propietario != null ? propietario.getNombres() + " " + propietario.getApellidos() : "Sin cliente");
        respuesta.put("veterinario", veterinario != null ? veterinario.getNombres() + " " + veterinario.getApellidos() : "Sin veterinario");
        respuesta.put("tipo", consulta.getTipoEvento() == null ? "Consulta" : consulta.getTipoEvento());
        respuesta.put("fecha", consulta.getFecha() == null ? "" : consulta.getFecha().toString());
        respuesta.put("diagnostico", consulta.getDiagnostico() == null ? "" : consulta.getDiagnostico());
        respuesta.put("tratamiento", consulta.getTratamiento() == null ? "" : consulta.getTratamiento());
        respuesta.put("pesoKg", mascota != null && mascota.getPesoKg() != null ? mascota.getPesoKg() : 0);
        respuesta.put("temperatura", consulta.getTemperatura() == null ? 0 : consulta.getTemperatura());
        respuesta.put("precio", precio);
        respuesta.put("facturaId", consulta.getFacturaId());
        return respuesta;
    }

    @Transactional
    public HistorialClinico crear(HistorialClinico consulta) {
        // El identificador lo asigna el servidor: si el cliente lo envia, se
        // descarta para que una alta nunca pueda sobrescribir otra consulta.
        consulta.setId(null);
        if (consulta.getPrecio() == null) {
            consulta.setPrecio(tarifaService.precioPorTipo(consulta.getTipoEvento()));
        }
        if (consulta.getFecha() == null) {
            consulta.setFecha(LocalDateTime.now());
        }
        if (consulta.getMascota() != null && consulta.getMascota().getId() != null) {
            Mascota mascota = mascotaRepository.findById(consulta.getMascota().getId()).orElse(null);
            if (mascota != null) {
                consulta.setMascota(mascota);
            }
        }
        return historialRepository.save(consulta);
    }

    @Transactional
    public HistorialClinico editar(Long id, Map<String, Object> datos) {
        HistorialClinico consulta = historialRepository.findById(id).orElseThrow();
        if (datos.containsKey("tipo")) consulta.setTipoEvento(texto(datos.get("tipo")));
        if (datos.containsKey("tipoEvento")) consulta.setTipoEvento(texto(datos.get("tipoEvento")));
        if (datos.containsKey("motivo")) consulta.setMotivo(texto(datos.get("motivo")));
        if (datos.containsKey("diagnostico")) consulta.setDiagnostico(texto(datos.get("diagnostico")));
        if (datos.containsKey("tratamiento")) consulta.setTratamiento(texto(datos.get("tratamiento")));
        if (datos.containsKey("vacunas")) consulta.setVacunas(texto(datos.get("vacunas")));
        if (datos.containsKey("temperatura")) consulta.setTemperatura(decimal(datos.get("temperatura")));
        if (datos.containsKey("frecuenciaCardiaca")) consulta.setFrecuenciaCardiaca(entero(datos.get("frecuenciaCardiaca")));
        if (datos.containsKey("frecuenciaRespiratoria")) consulta.setFrecuenciaRespiratoria(entero(datos.get("frecuenciaRespiratoria")));
        if (datos.containsKey("precio") && datos.get("precio") != null) {
            consulta.setPrecio(new BigDecimal(datos.get("precio").toString()));
        }
        return historialRepository.save(consulta);
    }

    @Transactional
    public void eliminar(Long id) {
        historialRepository.deleteById(id);
    }

    private String texto(Object valor) {
        return valor == null ? null : valor.toString();
    }

    private Double decimal(Object valor) {
        if (valor == null || valor.toString().isBlank()) return null;
        return Double.valueOf(valor.toString());
    }

    private Integer entero(Object valor) {
        if (valor == null || valor.toString().isBlank()) return null;
        return Integer.valueOf(valor.toString());
    }
}
