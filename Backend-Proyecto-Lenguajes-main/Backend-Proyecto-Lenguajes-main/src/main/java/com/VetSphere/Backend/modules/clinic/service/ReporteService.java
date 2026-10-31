package com.VetSphere.Backend.modules.clinic.service;

import com.VetSphere.Backend.modules.clinic.domain.Mascota;
import com.VetSphere.Backend.modules.clinic.repository.MascotaRepository;
import com.VetSphere.Backend.modules.clinic.repository.ProductoRepository;
import com.VetSphere.Backend.modules.clinic.domain.Hospitalizacion;
import com.VetSphere.Backend.modules.clinic.domain.OrdenLaboratorio;
import com.VetSphere.Backend.modules.clinic.repository.HospitalizacionRepository;
import com.VetSphere.Backend.modules.clinic.repository.OrdenLaboratorioRepository;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReporteService {
    private final MascotaRepository mascotaRepository;
    private final ProductoRepository productoRepository;
    private final HospitalizacionRepository hospitalizacionRepository;
    private final OrdenLaboratorioRepository ordenLaboratorioRepository;

    public byte[] historialPdf(Long mascotaId) {
        Mascota mascota = mascotaRepository.findById(mascotaId).orElseThrow();
        List<Hospitalizacion> hospitalizaciones = hospitalizacionRepository.findByMascotaId(mascotaId);
        List<OrdenLaboratorio> ordenesLaboratorio = ordenLaboratorioRepository.findByMascotaId(mascotaId);
        
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4, 34, 34, 34, 34);
        PdfWriter.getInstance(doc, out);
        doc.open();
        
        // Premium fonts resembling the billing system styling
        Font titulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, Color.WHITE);
        Font subtitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.WHITE);
        Font smallWhite = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.WHITE);
        Font h2 = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, new Color(15, 23, 42)); // Slate 900
        Font body = FontFactory.getFont(FontFactory.HELVETICA, 9, new Color(51, 65, 85));

        // Receipt header layout
        PdfPTable cabecera = new PdfPTable(new float[]{1.3f, 3.7f, 1.5f});
        cabecera.setWidthPercentage(100);
        cabecera.addCell(celdaLogo());
        cabecera.addCell(celdaCabecera("VetSphere", "Clínica Veterinaria Integral\nHistorial Clínico Consolidado", titulo, subtitulo));
        cabecera.addCell(celdaCabecera("Código Paciente", "HC-" + String.format("%04d", mascota.getId()) + "\nEmitido automáticamente", subtitulo, smallWhite));
        doc.add(cabecera);
        doc.add(espacio(12));

        // Details grid boxes
        PdfPTable ficha = new PdfPTable(new float[]{1, 1, 1, 1});
        ficha.setWidthPercentage(100);
        ficha.addCell(celdaDato("Paciente", valor(mascota.getNombre()), body));
        ficha.addCell(celdaDato("Especie / Raza", valor(mascota.getEspecie()) + " - " + valor(mascota.getRaza()), body));
        ficha.addCell(celdaDato("Peso / Color", (mascota.getPesoKg() != null ? mascota.getPesoKg() : 0.0) + " kg - " + valor(mascota.getColor()), body));
        ficha.addCell(celdaDato("Estado Paciente", valor(mascota.getEstado()), body));
        ficha.addCell(celdaDato("Propietario", mascota.getPropietario() != null ? mascota.getPropietario().getNombres() + " " + mascota.getPropietario().getApellidos() : "Sin propietario", body));
        ficha.addCell(celdaDato("Teléfono", mascota.getPropietario() != null ? valor(mascota.getPropietario().getTelefono()) : "N/A", body));
        ficha.addCell(celdaDato("Correo", mascota.getPropietario() != null ? valor(mascota.getPropietario().getEmail()) : "N/A", body));
        ficha.addCell(celdaDato("Nacimiento", valor(mascota.getFechaNacimiento()), body));
        doc.add(ficha);
        doc.add(espacio(16));

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        // 1. Consultation list
        doc.add(tituloSeccion("Consultas, vacunas y eventos médicos", h2));
        PdfPTable eventos = tabla(new float[]{1.25f, 1f, 2f, 2.4f, 1.2f});
        encabezados(eventos, "Fecha", "Tipo", "Motivo", "Diagnóstico y tratamiento", "Signos");
        if (mascota.getHistoriales().isEmpty()) {
            eventos.addCell(celdaVacia("Sin eventos clínicos registrados", 5));
        } else {
            for (var h : mascota.getHistoriales()) {
                eventos.addCell(celdaTexto(h.getFecha().format(fmt), body));
                eventos.addCell(celdaTexto(valor(h.getTipoEvento()), body));
                eventos.addCell(celdaTexto(valor(h.getMotivo()), body));
                eventos.addCell(celdaTexto("Dx: " + valor(h.getDiagnostico()) + "\nTx: " + valor(h.getTratamiento()), body));
                eventos.addCell(celdaTexto("Temp: " + (h.getTemperatura() != null ? h.getTemperatura() : "N/A") + " °C\nFC: " + (h.getFrecuenciaCardiaca() != null ? h.getFrecuenciaCardiaca() : "N/A") + "\nFR: " + (h.getFrecuenciaRespiratoria() != null ? h.getFrecuenciaRespiratoria() : "N/A"), body));
            }
        }
        doc.add(eventos);
        doc.add(espacio(14));

        // 2. Surgery list
        doc.add(tituloSeccion("Cirugías y procedimientos", h2));
        PdfPTable cirugias = tabla(new float[]{1.25f, 2f, 1.5f, 1f, 2.2f});
        encabezados(cirugias, "Fecha", "Procedimiento", "Anestesia", "Estado", "Postoperatorio");
        if (mascota.getCirugias().isEmpty()) {
            cirugias.addCell(celdaVacia("Sin cirugías registradas", 5));
        } else {
            for (var c : mascota.getCirugias()) {
                cirugias.addCell(celdaTexto(c.getFechaProgramada().format(fmt), body));
                cirugias.addCell(celdaTexto(valor(c.getProcedimiento()), body));
                cirugias.addCell(celdaTexto(valor(c.getTipoAnestesia()), body));
                cirugias.addCell(celdaTexto(valor(c.getEstado()), body));
                cirugias.addCell(celdaTexto("Constantes: " + valor(c.getConstantesVitales()) + "\n" + valor(c.getEstadoPostoperatorio()), body));
            }
        }
        doc.add(cirugias);
        doc.add(espacio(14));

        // 3. Hospitalization list
        doc.add(tituloSeccion("Hospitalizaciones y cuidados", h2));
        PdfPTable hospTable = tabla(new float[]{1.3f, 1.3f, 0.8f, 2f, 2.6f});
        encabezados(hospTable, "Ingreso", "Alta", "Jaula", "Motivo", "Plan de cuidados");
        if (hospitalizaciones.isEmpty()) {
            hospTable.addCell(celdaVacia("Sin hospitalizaciones registradas", 5));
        } else {
            for (var h : hospitalizaciones) {
                hospTable.addCell(celdaTexto(h.getIngreso().format(fmt), body));
                hospTable.addCell(celdaTexto(h.getAlta() != null ? h.getAlta().format(fmt) : "En curso", body));
                hospTable.addCell(celdaTexto(valor(h.getJaula()), body));
                hospTable.addCell(celdaTexto(valor(h.getMotivo()), body));
                hospTable.addCell(celdaTexto(valor(h.getPlanCuidados()), body));
            }
        }
        doc.add(hospTable);
        doc.add(espacio(14));

        // 4. Lab orders list
        doc.add(tituloSeccion("Órdenes de laboratorio y exámenes", h2));
        PdfPTable labTable = tabla(new float[]{1.3f, 2.2f, 3.2f, 1.3f});
        encabezados(labTable, "Fecha", "Tipo de prueba", "Resultado / Observaciones", "Estado");
        if (ordenesLaboratorio.isEmpty()) {
            labTable.addCell(celdaVacia("Sin análisis clínicos registrados", 4));
        } else {
            for (var o : ordenesLaboratorio) {
                labTable.addCell(celdaTexto(o.getFecha().format(fmt), body));
                labTable.addCell(celdaTexto(valor(o.getTipoPrueba()), body));
                labTable.addCell(celdaTexto(valor(o.getResultado()), body));
                labTable.addCell(celdaTexto(valor(o.getEstado()), body));
            }
        }
        doc.add(labTable);
        doc.add(espacio(16));

        // PDF Footer resembling the bill receipt
        PdfPTable pie = new PdfPTable(new float[]{2.2f, 1f});
        pie.setWidthPercentage(100);
        pie.addCell(celdaNota("Este documento contiene el historial clínico unificado del paciente y está vinculado directamente con el sistema de facturación. Conservar para fines médicos y administrativos.", body));
        pie.addCell(celdaFirma(body));
        doc.add(pie);

        doc.close();
        return out.toByteArray();
    }

    private PdfPTable tabla(float[] anchos) {
        PdfPTable tabla = new PdfPTable(anchos);
        tabla.setWidthPercentage(100);
        tabla.setSpacingBefore(8);
        return tabla;
    }

    private void encabezados(PdfPTable tabla, String... textos) {
        Font font = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.WHITE);
        for (String texto : textos) {
            PdfPCell celda = new PdfPCell(new Phrase(texto, font));
            celda.setPadding(8);
            celda.setBackgroundColor(new Color(14, 116, 144));
            celda.setBorderColor(new Color(14, 116, 144));
            tabla.addCell(celda);
        }
    }

    private PdfPCell celdaLogo() {
        Font font = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, Color.WHITE);
        PdfPCell celda = new PdfPCell(new Phrase("VS", font));
        celda.setHorizontalAlignment(Element.ALIGN_CENTER);
        celda.setVerticalAlignment(Element.ALIGN_MIDDLE);
        celda.setPadding(18);
        celda.setBackgroundColor(new Color(7, 20, 38));
        celda.setBorderColor(new Color(7, 20, 38));
        return celda;
    }

    private PdfPCell celdaCabecera(String titulo, String texto, Font tituloFont, Font textoFont) {
        Paragraph parrafo = new Paragraph();
        parrafo.add(new Chunk(titulo + "\n", tituloFont));
        parrafo.add(new Chunk(texto, textoFont));
        PdfPCell celda = new PdfPCell(parrafo);
        celda.setPadding(16);
        celda.setBackgroundColor(new Color(8, 145, 178));
        celda.setBorderColor(new Color(8, 145, 178));
        return celda;
    }

    private PdfPCell celdaDato(String etiqueta, String valor, Font body) {
        Font label = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7, new Color(14, 116, 144));
        Paragraph p = new Paragraph();
        p.add(new Chunk(etiqueta.toUpperCase() + "\n", label));
        p.add(new Chunk(valor, body));
        PdfPCell celda = new PdfPCell(p);
        celda.setPadding(9);
        celda.setBackgroundColor(new Color(245, 251, 253));
        celda.setBorderColor(new Color(219, 232, 240));
        return celda;
    }

    private PdfPCell celdaTexto(String texto, Font font) {
        PdfPCell celda = new PdfPCell(new Phrase(texto, font));
        celda.setPadding(8);
        celda.setBorderColor(new Color(219, 232, 240));
        return celda;
    }

    private PdfPCell celdaVacia(String texto, int columnas) {
        Font font = FontFactory.getFont(FontFactory.HELVETICA, 9, new Color(100, 116, 139));
        PdfPCell celda = celdaTexto(texto, font);
        celda.setColspan(columnas);
        celda.setHorizontalAlignment(Element.ALIGN_CENTER);
        return celda;
    }

    private PdfPCell celdaNota(String texto, Font font) {
        PdfPCell celda = celdaTexto(texto, font);
        celda.setPadding(14);
        celda.setBackgroundColor(new Color(236, 254, 255));
        return celda;
    }

    private PdfPCell celdaFirma(Font font) {
        PdfPCell celda = celdaTexto("\n\nFirma autorizada\nVetSphere", font);
        celda.setHorizontalAlignment(Element.ALIGN_CENTER);
        celda.setVerticalAlignment(Element.ALIGN_BOTTOM);
        return celda;
    }

    private Paragraph tituloSeccion(String texto, Font font) {
        Paragraph p = new Paragraph(texto, font);
        p.setSpacingBefore(4);
        p.setSpacingAfter(4);
        return p;
    }

    private Paragraph espacio(int alto) {
        Paragraph p = new Paragraph(" ");
        p.setSpacingBefore(alto / 2f);
        p.setSpacingAfter(alto / 2f);
        return p;
    }

    private String valor(Object valor) {
        if (valor == null) return "Sin dato";
        String texto = String.valueOf(valor);
        return texto.isBlank() ? "Sin dato" : texto;
    }

    /**
     * Listado de mascotas en formato CSV UTF-8 con BOM (para que Excel abra
     * los acentos correctamente). Columnas: Id, Nombre, Especie, Raza, Peso,
     * Estado y Cliente (dueno).
     */
    public byte[] mascotasCsv() {
        StringBuilder csv = new StringBuilder("\uFEFF");
        csv.append("Id,Nombre,Especie,Raza,Peso,Estado,Cliente\r\n");
        for (Mascota mascota : mascotaRepository.findAll()) {
            csv.append(columna(mascota.getId()))
                    .append(',').append(columna(mascota.getNombre()))
                    .append(',').append(columna(mascota.getEspecie()))
                    .append(',').append(columna(mascota.getRaza()))
                    .append(',').append(columna(mascota.getPesoKg()))
                    .append(',').append(columna(mascota.getEstado()))
                    .append(',').append(columna(clienteDe(mascota)))
                    .append("\r\n");
        }
        return csv.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    private String clienteDe(Mascota mascota) {
        if (mascota.getPropietario() == null) {
            return "Sin cliente";
        }
        String nombres = mascota.getPropietario().getNombres();
        String apellidos = mascota.getPropietario().getApellidos();
        return ((nombres == null ? "" : nombres) + " " + (apellidos == null ? "" : apellidos)).trim();
    }

    /** Escapa una columna CSV: si trae comas, comillas o saltos de linea va entre comillas. */
    private String columna(Object valor) {
        if (valor == null) {
            return "";
        }
        String texto = String.valueOf(valor);
        if (texto.contains(",") || texto.contains("\"") || texto.contains("\n") || texto.contains("\r")) {
            return "\"" + texto.replace("\"", "\"\"") + "\"";
        }
        return texto;
    }

    public byte[] financieroExcel() {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("Finanzas VetSphere");
            var header = sheet.createRow(0);
            header.createCell(0).setCellValue("Codigo");
            header.createCell(1).setCellValue("Producto");
            header.createCell(2).setCellValue("Categoria");
            header.createCell(3).setCellValue("Stock");
            header.createCell(4).setCellValue("Precio");
            header.createCell(5).setCellValue("Valor inventario");
            int rowIndex = 1;
            for (var p : productoRepository.findAll()) {
                var row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(p.getCodigo());
                row.createCell(1).setCellValue(p.getNombre());
                row.createCell(2).setCellValue(p.getCategoria());
                row.createCell(3).setCellValue(p.getStock());
                row.createCell(4).setCellValue(p.getPrecio().doubleValue());
                row.createCell(5).setCellValue(p.getPrecio().doubleValue() * p.getStock());
            }
            for (int i = 0; i < 6; i++) sheet.autoSizeColumn(i);
            workbook.write(out);
            return out.toByteArray();
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo generar Excel", ex);
        }
    }
}


