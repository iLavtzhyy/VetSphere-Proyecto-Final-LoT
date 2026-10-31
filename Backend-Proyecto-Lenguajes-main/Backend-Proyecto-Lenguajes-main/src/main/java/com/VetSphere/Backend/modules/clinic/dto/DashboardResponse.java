package com.VetSphere.Backend.modules.clinic.dto;

import com.VetSphere.Backend.modules.clinic.domain.Producto;
import java.util.List;
import java.util.Map;

public record DashboardResponse(
        long totalClientes,
        long totalMascotas,
        long mascotasInternadas,
        long citasHoy,
        long citasMes,
        long cirugiasHoy,
        long totalConsultas,
        long totalCirugias,
        long totalVacunas,
        long totalLaboratorio,
        long totalProductos,
        long totalEmergencias,
        List<Integer> citasUltimos7Dias,
        long totalPerros,
        long totalGatos,
        List<Map<String, Object>> cirugiasRecientes,
        List<Producto> stockCritico
) {
}
