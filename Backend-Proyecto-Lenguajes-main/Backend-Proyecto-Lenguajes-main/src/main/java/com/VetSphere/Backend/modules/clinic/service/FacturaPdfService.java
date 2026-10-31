package com.VetSphere.Backend.modules.clinic.service;

import com.VetSphere.Backend.exception.ResourceNotFoundException;
import com.VetSphere.Backend.modules.clinic.domain.Factura;
import com.VetSphere.Backend.modules.clinic.domain.FacturaDetalle;
import com.VetSphere.Backend.modules.clinic.repository.CirugiaRepository;
import com.VetSphere.Backend.modules.clinic.repository.FacturaDetalleRepository;
import com.VetSphere.Backend.modules.clinic.repository.FacturaRepository;
import com.VetSphere.Backend.modules.clinic.repository.HistorialClinicoRepository;
import com.VetSphere.Backend.modules.clinic.repository.HospitalizacionRepository;
import com.VetSphere.Backend.modules.clinic.repository.OrdenLaboratorioRepository;
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Genera el PDF de la factura (comprobante de venta) con los datos REALES
 * guardados en base de datos: nunca se confia en valores enviados por el
 * cliente.
 *
 * El documento incluye:
 * <ul>
 *   <li>Encabezado con los datos de la clinica (configurables) y el numero de factura.</li>
 *   <li>Bloques emisor / receptor con los datos del cliente y la mascota.</li>
 *   <li>Tabla de detalle: codigo, descripcion, cantidad, precio unitario, descuento e importe.</li>
 *   <li>Totales: base imponible, descuento aplicado (con codigo de cupon), IVA 15% y total.</li>
 *   <li>Total en letras, forma de pago, estado y observaciones.</li>
 *   <li>Terminos y condiciones, linea de firma y codigo de verificacion.</li>
 *   <li>Numeracion de paginas y marca de agua ANULADO cuando corresponde.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class FacturaPdfService {

    /** Resultado de la generacion: nombre sugerido de descarga y contenido. */
    public record FacturaPdf(String nombreArchivo, byte[] contenido) {
    }

    /** Linea mostrada en la tabla de detalle. */
    private record LineaPdf(String codigo, String descripcion, int cantidad,
                            BigDecimal precioUnitario, BigDecimal descuento, BigDecimal importe) {
    }

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    /** Fuentes base para el pie de pagina y la marca de agua. */
    private static final BaseFont FUENTE_PIE = crearFuente(FontFactory.HELVETICA);
    private static final BaseFont FUENTE_MARCA_AGUA = crearFuente(FontFactory.HELVETICA_BOLD);

    /** Crea una fuente base estandar de PDF (no incrustada). */
    private static BaseFont crearFuente(String nombre) {
        try {
            return BaseFont.createFont(nombre, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
        } catch (Exception ex) {
            return null;
        }
    }

    private static final Color COLOR_CABECERA = new Color(22, 28, 38);
    private static final Color COLOR_ENCABEZADO_TABLA = new Color(47, 55, 66);
    private static final Color COLOR_BORDE = new Color(220, 225, 232);
    private static final Color COLOR_ETIQUETA = new Color(83, 91, 102);
    private static final Color COLOR_TOTAL = new Color(238, 241, 245);
    private static final Color COLOR_ALERTA = new Color(190, 18, 60);        // Rose 700
    private static final Color COLOR_ZONA = new Color(247, 248, 250);

    private final FacturaRepository facturaRepository;
    private final FacturaDetalleRepository detalleRepository;
    private final HistorialClinicoRepository historialRepository;
    private final CirugiaRepository cirugiaRepository;
    private final OrdenLaboratorioRepository ordenRepository;
    private final HospitalizacionRepository hospitalizacionRepository;

    @Value("${app.clinica.nombre:VetSphere - Clinica Veterinaria Integral}")
    private String clinicaNombre;

    @Value("${app.clinica.ruc:}")
    private String clinicaRuc;

    @Value("${app.clinica.direccion:Av. Cristobal Colon E10-45 y Av. 12 de Octubre, Quito (La Floresta)}")
    private String clinicaDireccion;

    @Value("${app.clinica.telefono:0999999999}")
    private String clinicaTelefono;

    @Value("${app.clinica.email:contacto@vetsphere.com}")
    private String clinicaEmail;

    @Value("${app.clinica.web:www.vetsphere.com}")
    private String clinicaWeb;

    @Value("${app.clinica.licencia:}")
    private String clinicaLicencia;

    @Value("${app.moneda:USD}")
    private String moneda;

    /**
     * Construye el PDF de la factura indicada.
     *
     * @throws ResourceNotFoundException si la factura no existe
     */
    public FacturaPdf generarPdf(Long facturaId) {
        Factura factura = facturaRepository.findById(facturaId)
                .orElseThrow(() -> new ResourceNotFoundException("Factura no encontrada"));

        ByteArrayOutputStream flujo = new ByteArrayOutputStream();
        try {
            Document documento = new Document(PageSize.LETTER, 42, 42, 38, 48);
            PdfWriter escritor = PdfWriter.getInstance(documento, flujo);
            escritor.setCompressionLevel(9);
            boolean anulada = "Anulado".equalsIgnoreCase(factura.getEstadoPago());
            escritor.setPageEvent(new PieDePagina(anulada));

            documento.open();
            documento.addTitle("Factura " + valor(factura.getNumero()) + " - " + clinicaNombre);
            documento.addSubject("Comprobante veterinario para " + cliente(factura));
            documento.addCreator("VetSphere");

            documento.add(cabecera(factura));
            documento.add(espacio(10));
            documento.add(bloqueResumen(factura));
            documento.add(espacio(12));
            documento.add(tablaItems(lineas(factura)));
            documento.add(espacio(10));
            documento.add(bloqueTotales(factura));
            documento.add(espacio(14));
            documento.add(mensajeCierre());

            documento.close();
        } catch (DocumentException ex) {
            throw new IllegalStateException("No se pudo generar el PDF de la factura " + factura.getNumero(), ex);
        }
        return new FacturaPdf(nombreArchivo(factura), flujo.toByteArray());
    }

    // ------------------------------------------------------------------
    // Bloques del documento
    // ------------------------------------------------------------------

    /** Encabezado breve con identidad de la clinica y datos principales de la factura. */
    private PdfPTable cabecera(Factura factura) {
        Font titulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 15, Color.WHITE);
        Font subtitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, COLOR_ETIQUETA);
        Font dato = FontFactory.getFont(FontFactory.HELVETICA, 8, COLOR_ETIQUETA);

        PdfPTable cabecera = new PdfPTable(new float[]{0.72f, 2.35f, 1.45f});
        cabecera.setWidthPercentage(100);
        cabecera.setSpacingAfter(2);

        Paragraph izquierda = new Paragraph();
        izquierda.setLeading(16f);
        izquierda.add(new Chunk(clinicaNombre + "\n", titulo));
        izquierda.add(new Chunk("CLÍNICA VETERINARIA INTEGRAL\n", subtitulo));
        izquierda.add(new Chunk(linea(clinicaDireccion) + "\n", dato));
        izquierda.add(new Chunk(clinicaTelefono + "  |  " + clinicaEmail, dato));
        PdfPCell celdaLogo = new PdfPCell();
        celdaLogo.setPadding(8);
        celdaLogo.setBackgroundColor(Color.WHITE);
        celdaLogo.setBorderColor(Color.WHITE);
        celdaLogo.setVerticalAlignment(Element.ALIGN_MIDDLE);
        celdaLogo.setHorizontalAlignment(Element.ALIGN_CENTER);
        Image logo = cargarLogo();
        logo.scaleToFit(50f, 50f);
        celdaLogo.addElement(logo);
        cabecera.addCell(celdaLogo);

        PdfPCell celdaIzquierda = new PdfPCell(izquierda);
        celdaIzquierda.setPadding(12);
        celdaIzquierda.setBackgroundColor(Color.WHITE);
        celdaIzquierda.setBorderColor(Color.WHITE);
        celdaIzquierda.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cabecera.addCell(celdaIzquierda);

        Paragraph derecha = new Paragraph();
        derecha.setLeading(16f);
        derecha.setAlignment(Element.ALIGN_RIGHT);
        boolean anulada = "Anulado".equalsIgnoreCase(factura.getEstadoPago());
        derecha.add(new Chunk("FACTURA\n", subtitulo));
        derecha.add(new Chunk(valor(factura.getNumero()) + "\n",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, COLOR_CABECERA)));
        derecha.add(new Chunk("Fecha: " + fecha(factura) + "\n", dato));
        derecha.add(new Chunk(estado(factura).toUpperCase(),
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, COLOR_ETIQUETA)));
        PdfPCell celdaDerecha = new PdfPCell(derecha);
        celdaDerecha.setPadding(12);
        celdaDerecha.setHorizontalAlignment(Element.ALIGN_RIGHT);
        celdaDerecha.setVerticalAlignment(Element.ALIGN_MIDDLE);
        celdaDerecha.setBackgroundColor(Color.WHITE);
        celdaDerecha.setBorderColor(Color.WHITE);
        cabecera.addCell(celdaDerecha);

        return cabecera;
    }

    private Image cargarLogo() {
        try {
            java.net.URL recurso = FacturaPdfService.class.getResource("/logo-vetsphere-icon.png");
            if (recurso == null) {
                throw new IllegalStateException("No se encontro el logo de VetSphere para la factura");
            }
            return Image.getInstance(recurso);
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo cargar el logo de VetSphere para la factura", ex);
        }
    }

    /** Resumen en tarjetas breves, similar a un recibo moderno. */
    private PdfPTable bloqueResumen(Factura factura) {
        PdfPTable tabla = new PdfPTable(new float[]{1f, 1f});
        tabla.setWidthPercentage(100);
        tabla.setSpacingAfter(2);
        String detalleCliente = "";
        if (factura.getCliente() != null) {
            String documento = factura.getCliente().getDocumento();
            String correo = factura.getCliente().getEmail();
            detalleCliente = documento != null && !documento.isBlank() ? "C.I. " + documento : "";
            if (correo != null && !correo.isBlank()) {
                detalleCliente += detalleCliente.isBlank() ? correo : "  |  " + correo;
            }
        }
        tabla.addCell(tarjeta("CLIENTE", cliente(factura), detalleCliente));
        tabla.addCell(tarjeta("FECHA", fecha(factura), "Emisión de la factura"));
        tabla.addCell(tarjeta("PACIENTE", mascota(factura), "Atención veterinaria"));
        tabla.addCell(tarjeta("ESTADO", estado(factura), ""));
        tabla.addCell(tarjeta("MÉTODO DE PAGO", valor(factura.getMetodoPago()), ""));
        tabla.addCell(tarjeta("CUPÓN", factura.getCuponCodigo() == null || factura.getCuponCodigo().isBlank()
                ? "Sin cupón" : factura.getCuponCodigo(), ""));

        return tabla;
    }

    private PdfPCell tarjeta(String etiqueta, String valor, String detalle) {
        Font etiquetaFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, COLOR_ETIQUETA);
        Font valorFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, COLOR_CABECERA);
        Font detalleFont = FontFactory.getFont(FontFactory.HELVETICA, 8, COLOR_ETIQUETA);
        Paragraph contenido = new Paragraph();
        contenido.setLeading(14f);
        contenido.add(new Chunk(etiqueta + "\n", etiquetaFont));
        contenido.add(new Chunk(valor == null ? "-" : valor, valorFont));
        if (detalle != null && !detalle.isBlank()) {
            contenido.add(new Chunk("\n" + detalle, detalleFont));
        }
        PdfPCell celda = celdaBloque(contenido);
        celda.setPadding(11);
        return celda;
    }

    /** Tabla de detalle de la factura. */
    private PdfPTable tablaItems(List<LineaPdf> items) {
        Font encabezadoFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.WHITE);
        Font textoFont = FontFactory.getFont(FontFactory.HELVETICA, 9, COLOR_ETIQUETA);
        Font textoCentro = FontFactory.getFont(FontFactory.HELVETICA, 9, COLOR_ETIQUETA);

        PdfPTable tabla = new PdfPTable(new float[]{3.7f, 0.65f, 1.15f, 1.0f, 1.25f});
        tabla.setWidthPercentage(100);
        tabla.setHeaderRows(1);
        tabla.setSpacingBefore(4);
        tabla.setSpacingAfter(3);
        tabla.setSplitRows(true);
        tabla.addCell(encabezadoTabla("DETALLE DEL SERVICIO / PRODUCTO", encabezadoFont, Element.ALIGN_LEFT));
        tabla.addCell(encabezadoTabla("Cant.", encabezadoFont, Element.ALIGN_CENTER));
        tabla.addCell(encabezadoTabla("P. unitario", encabezadoFont, Element.ALIGN_RIGHT));
        tabla.addCell(encabezadoTabla("Descuento", encabezadoFont, Element.ALIGN_RIGHT));
        tabla.addCell(encabezadoTabla("Importe", encabezadoFont, Element.ALIGN_RIGHT));

        int fila = 0;
        for (LineaPdf item : items) {
            PdfPCell descripcion = celdaTexto(valor(item.descripcion()), textoFont, Element.ALIGN_LEFT);
            if ((fila++ % 2) == 1) {
                descripcion.setBackgroundColor(COLOR_ZONA);
            }
            tabla.addCell(descripcion);
            tabla.addCell(celdaTexto(String.valueOf(item.cantidad()), textoCentro, Element.ALIGN_CENTER));
            tabla.addCell(celdaTexto(dinero(item.precioUnitario()), textoFont, Element.ALIGN_RIGHT));
            tabla.addCell(celdaTexto(item.descuento() != null && item.descuento().signum() > 0
                    ? "-" + dinero(item.descuento()) : "-", textoFont, Element.ALIGN_RIGHT));
            tabla.addCell(celdaTexto(dinero(item.importe()), textoFont, Element.ALIGN_RIGHT));
        }
        return tabla;
    }

    /** Totales de la factura. */
    private PdfPTable bloqueTotales(Factura factura) {
        Font etiquetaFont = FontFactory.getFont(FontFactory.HELVETICA, 10, COLOR_ETIQUETA);
        Font totalFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, COLOR_CABECERA);
        Font subTotalFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, COLOR_CABECERA);

        PdfPTable tabla = new PdfPTable(2);
        tabla.setWidthPercentage(49);
        tabla.setHorizontalAlignment(Element.ALIGN_RIGHT);

        agregarFilaTotal(tabla, "Subtotal", dinero(bruto(factura)), etiquetaFont, false);
        if (factura.descuentoEfectivo().signum() > 0) {
            String etiqueta = "Descuentos";
            if (factura.getCuponCodigo() != null && !factura.getCuponCodigo().isBlank()) {
                etiqueta = "Descuento (cupon " + factura.getCuponCodigo() + ")";
            }
            agregarFilaTotal(tabla, etiqueta, "-" + dinero(factura.descuentoEfectivo()), etiquetaFont, false);
        }
        agregarFilaTotal(tabla, "Base imponible", dinero(factura.getSubtotal()), subTotalFont, false);
        agregarFilaTotal(tabla, "IVA (15%)", dinero(factura.getImpuestos()), etiquetaFont, false);
        agregarFilaTotal(tabla, "TOTAL " + moneda, dinero(factura.getTotal()), totalFont, true);
        return tabla;
    }

    private Paragraph mensajeCierre() {
        Paragraph mensaje = new Paragraph("Gracias por confiar en VetSphere. Conserva este comprobante como respaldo de la atención de tu mascota.",
                FontFactory.getFont(FontFactory.HELVETICA, 9, COLOR_ETIQUETA));
        mensaje.setAlignment(Element.ALIGN_CENTER);
        mensaje.setSpacingBefore(3);
        return mensaje;
    }

    // ------------------------------------------------------------------
    // Lineas de la factura
    // ------------------------------------------------------------------

    /**
     * Lineas reales de la factura. Se prioriza el detalle persistido
     * (productos y servicios facturados) y, si la factura es anterior a la
     * implementacion del detalle, se reconstruye con los servicios clinicos
     * vinculados o con el concepto de la factura.
     */
    private List<LineaPdf> lineas(Factura factura) {
        Long id = factura.getId();
        List<LineaPdf> lineas = new ArrayList<>();

        for (FacturaDetalle detalle : detalleRepository.findByFacturaIdOrderByIdAsc(id)) {
            String codigo = detalle.getProducto() != null ? detalle.getProducto().getCodigo() : "";
            String descripcion = detalle.getDescripcion() != null ? detalle.getDescripcion()
                    : (detalle.getProducto() != null ? detalle.getProducto().getNombre() : "Concepto");
            lineas.add(new LineaPdf(codigo, descripcion,
                    detalle.getCantidad() == null ? 1 : detalle.getCantidad(),
                    detalle.getPrecioUnitario(), detalle.getDescuento(), detalle.getSubtotal()));
        }
        if (!lineas.isEmpty()) {
            return lineas;
        }

        historialRepository.findByFacturaId(id).forEach(h ->
                lineas.add(new LineaPdf("", describir("Consulta", h.getTipoEvento(), h.getMotivo()), 1,
                        h.getPrecio(), BigDecimal.ZERO, h.getPrecio())));
        cirugiaRepository.findByFacturaId(id).forEach(c ->
                lineas.add(new LineaPdf("", describir("Cirugia", c.getProcedimiento(), null), 1,
                        c.getPrecio(), BigDecimal.ZERO, c.getPrecio())));
        ordenRepository.findByFacturaId(id).forEach(o ->
                lineas.add(new LineaPdf("", describir("Laboratorio", o.getTipoPrueba(), null), 1,
                        o.getPrecio(), BigDecimal.ZERO, o.getPrecio())));
        hospitalizacionRepository.findByFacturaId(id).forEach(h ->
                lineas.add(new LineaPdf("", describir("Hospitalizacion", h.getMotivo(), null), 1,
                        h.getPrecio(), BigDecimal.ZERO, h.getPrecio())));

        if (lineas.isEmpty()) {
            String concepto = factura.getConcepto() == null || factura.getConcepto().isBlank()
                    ? "Servicio veterinario" : factura.getConcepto().trim();
            lineas.add(new LineaPdf("", concepto, 1, factura.getSubtotal(), BigDecimal.ZERO, factura.getSubtotal()));
        }
        return lineas;
    }

    /** Importe bruto de la factura (subtotal + descuentos). */
    private BigDecimal bruto(Factura factura) {
        BigDecimal subtotal = factura.getSubtotal() == null ? BigDecimal.ZERO : factura.getSubtotal();
        return subtotal.add(factura.descuentoEfectivo()).setScale(2, RoundingMode.HALF_UP);
    }

    private String describir(String categoria, String detalle, String complemento) {
        StringBuilder texto = new StringBuilder(categoria);
        if (detalle != null && !detalle.isBlank()) {
            texto.append(" - ").append(detalle.trim());
        }
        if (complemento != null && !complemento.isBlank()) {
            texto.append(" - ").append(complemento.trim());
        }
        return texto.toString();
    }

    // ------------------------------------------------------------------
    // Pie de pagina y marca de agua
    // ------------------------------------------------------------------

    /** Numeracion de paginas y marca de agua ANULADO. */
    private static class PieDePagina extends PdfPageEventHelper {
        private final boolean anulada;

        PieDePagina(boolean anulada) {
            this.anulada = anulada;
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            if (anulada && FUENTE_MARCA_AGUA != null) {
                PdfContentByte cb = writer.getDirectContent();
                cb.saveState();
                cb.setColorFill(new Color(226, 232, 240));
                cb.beginText();
                cb.setFontAndSize(FUENTE_MARCA_AGUA, 72);
                cb.showTextAligned(Element.ALIGN_CENTER, "ANULADO",
                        document.getPageSize().getWidth() / 2, document.getPageSize().getHeight() / 2, 0);
                cb.endText();
                cb.restoreState();
            }
            if (FUENTE_PIE == null) {
                return;
            }
            PdfContentByte pie = writer.getDirectContent();
            pie.saveState();
            pie.setColorFill(COLOR_ETIQUETA);
            pie.beginText();
            pie.setFontAndSize(FUENTE_PIE, 7);
            pie.showTextAligned(Element.ALIGN_LEFT, clinicaTexto(), 42, 28, 0);
            pie.showTextAligned(Element.ALIGN_CENTER, "Pagina " + writer.getPageNumber(), 306, 28, 0);
            pie.showTextAligned(Element.ALIGN_RIGHT, "Documento electronico - VetSphere", 570, 28, 0);
            pie.endText();
            pie.restoreState();
        }

        private String clinicaTexto() {
        return "VetSphere - Clinica Veterinaria Integral";
        }
    }

    // ------------------------------------------------------------------
    // Utilidades de formato
    // ------------------------------------------------------------------

    private PdfPCell encabezadoTabla(String texto, Font font, int alineacion) {
        PdfPCell celda = new PdfPCell(new Phrase(texto, font));
        celda.setPadding(7);
        celda.setBackgroundColor(COLOR_ENCABEZADO_TABLA);
        celda.setBorderColor(COLOR_ENCABEZADO_TABLA);
        celda.setHorizontalAlignment(alineacion);
        return celda;
    }

    private PdfPCell celdaTexto(String texto, Font font, int alineacion) {
        PdfPCell celda = new PdfPCell(new Phrase(texto == null ? "" : texto, font));
        celda.setPadding(7);
        celda.setBorderColor(COLOR_BORDE);
        celda.setHorizontalAlignment(alineacion);
        return celda;
    }

    private PdfPCell celdaBloque(Paragraph contenido) {
        PdfPCell celda = new PdfPCell(contenido);
        celda.setPadding(10);
        celda.setBorderColor(COLOR_BORDE);
        celda.setBackgroundColor(COLOR_ZONA);
        celda.setVerticalAlignment(Element.ALIGN_TOP);
        return celda;
    }

    private void agregarFilaTotal(PdfPTable tabla, String etiqueta, String importe, Font font, boolean destacado) {
        PdfPCell etiquetaCelda = new PdfPCell(new Phrase(etiqueta, font));
        PdfPCell importeCelda = new PdfPCell(new Phrase(importe, font));
        for (PdfPCell celda : new PdfPCell[]{etiquetaCelda, importeCelda}) {
            celda.setPadding(7);
            celda.setBorderColor(COLOR_BORDE);
            if (destacado) {
                celda.setBackgroundColor(COLOR_TOTAL);
            }
        }
        etiquetaCelda.setHorizontalAlignment(Element.ALIGN_LEFT);
        importeCelda.setHorizontalAlignment(Element.ALIGN_RIGHT);
        tabla.addCell(etiquetaCelda);
        tabla.addCell(importeCelda);
    }

    private Paragraph espacio(int puntos) {
        Paragraph parrafo = new Paragraph(" ");
        parrafo.setSpacingBefore(puntos / 2f);
        parrafo.setSpacingAfter(puntos / 2f);
        return parrafo;
    }

    private String fecha(Factura factura) {
        LocalDateTime fecha = factura.getFecha();
        return fecha == null ? "Sin fecha" : FORMATO_FECHA.format(fecha);
    }

    private String cliente(Factura factura) {
        if (factura.getCliente() == null) {
            return "Cliente general";
        }
        String nombres = valor(factura.getCliente().getNombres());
        String apellidos = valor(factura.getCliente().getApellidos());
        return (nombres + " " + apellidos).trim();
    }

    private String mascota(Factura factura) {
        return factura.getMascota() == null ? "Sin mascota" : valor(factura.getMascota().getNombre());
    }

    private String estado(Factura factura) {
        return factura.getEstadoPago() == null || factura.getEstadoPago().isBlank()
                ? "Pendiente" : factura.getEstadoPago();
    }

    private String dinero(BigDecimal valor) {
        BigDecimal cantidad = valor == null ? BigDecimal.ZERO : valor;
        return cantidad.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String valor(String texto) {
        return texto == null || texto.isBlank() ? "-" : texto.trim();
    }

    private String linea(String texto) {
        return texto == null ? "" : texto;
    }

    /** Nombre de descarga seguro: solo letras, numeros, guiones y guion bajo. */
    private String nombreArchivo(Factura factura) {
        String numero = factura.getNumero() == null ? factura.getId().toString() : factura.getNumero();
        return "factura-" + numero.replaceAll("[^A-Za-z0-9_-]", "_") + ".pdf";
    }
}
