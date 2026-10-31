package com.VetSphere.Backend.modules.clinic.controller;

import com.VetSphere.Backend.modules.clinic.service.ReporteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.Authentication;
import com.VetSphere.Backend.modules.clinic.service.MascotaService;

@RestController
@RequestMapping("/api/reportes")
@RequiredArgsConstructor
public class ReporteController {
    private final ReporteService reporteService;
    private final MascotaService mascotaService;

    @GetMapping("/mascotas/{id}/historial.pdf")
    public ResponseEntity<byte[]> historialPdf(@PathVariable Long id, Authentication authentication) {
        mascotaService.obtenerParaUsuario(id, authentication);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=historial-mascota-" + id + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(reporteService.historialPdf(id));
    }

    @GetMapping("/finanzas.xlsx")
    public ResponseEntity<byte[]> finanzasExcel() {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=reporte-financiero-vetsphere.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(reporteService.financieroExcel());
    }

    /**
     * Exportacion CSV de mascotas (UTF-8 con BOM para Excel). A diferencia de
     * los endpoints JSON, la respuesta es un archivo plano, igual que los
     * otros reportes del controlador.
     */
    @GetMapping("/mascotas-csv")
    public ResponseEntity<byte[]> mascotasCsv() {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=mascotas.csv")
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .body(reporteService.mascotasCsv());
    }
}


