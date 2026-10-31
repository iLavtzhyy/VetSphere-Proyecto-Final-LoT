package com.VetSphere.Backend.modules.clinic.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Triage veterinario por reglas (A -via aerea y consciencia-, B -circulacion-).
 *
 * Antes devolvia un nivel fijo salvo las convulsiones; ahora se evaluan las
 * constantes y los signos que llegan en la solicitud, se explican los motivos
 * y se propone el siguiente paso segun el nivel asignado.
 */
@Service
public class TriageService {

    /** Nivel de urgencia y color de la ficha de triage. */
    public enum Nivel {
        ROJO("Rojo", "Emergencia: atencion inmediata"),
        NARANJA("Naranja", "Muy urgente: atender en menos de 15 minutos"),
        AMARILLO("Amarillo", "Urgente: atender en menos de 60 minutos"),
        VERDE("Verde", "Estable: puede esperar su turno");

        private final String etiqueta;
        private final String descripcion;

        Nivel(String etiqueta, String descripcion) {
            this.etiqueta = etiqueta;
            this.descripcion = descripcion;
        }

        public String getEtiqueta() {
            return etiqueta;
        }

        public String getDescripcion() {
            return descripcion;
        }
    }

    /**
     * Resultado del triage: nivel, motivos y siguiente paso.
     */
    public record Resultado(Nivel nivel, List<String> motivos, String siguientePaso) {
    }

    /**
     * Clasifica al paciente segun los signos y constantes enviados.
     *
     * @param signos mapa con los signos SNR (sin normalizar, acepta tildes y mayusculas)
     */
    public Resultado clasificar(Map<String, Object> signos) {
        Map<String, Object> datos = normalizar(signos);
        List<String> reasonsRojo = new ArrayList<>();
        List<String> reasonsNaranja = new ArrayList<>();
        List<String> reasonsAmarillo = new ArrayList<>();

        // A. Via aerea y consciencia.
        if (booleano(datos, "noResponde") || booleano(datos, "inconsciente")) {
            reasonsRojo.add("El paciente no responde a los estimulos.");
        }
        if (booleano(datos, "convulsiones")) {
            reasonsRojo.add("Convulsiones en curso o recientes.");
        }
        if (booleano(datos, "dificultadRespiratoria") || booleano(datos, "estridor")) {
            reasonsRojo.add("Dificultad respiratoria o estridor.");
        }
        if (booleano(datos, "edemaLaringeo") || booleano(datos, "sangradoNasal")) {
            reasonsRojo.add("Riesgo de obstruccion de la via aerea.");
        }

        // B. Circulacion y hemorragia.
        if (booleano(datos, "hemorragiaActiva")) {
            reasonsRojo.add("Hemorragia activa.");
        }
        if (booleano(datos, "mucosasPalidas") || booleano(datos, "cianosis")) {
            reasonsRojo.add("Mucosas palidas o cianosis: perfusion deficiente.");
        }
        if (booleano(datos, "pulsoDebil")) {
            reasonsRojo.add("Pulso debil.");
        }
        if (booleano(datos, "trauma")) {
            reasonsNaranja.add("Trauma reciente: descartar lesion interna.");
        }
        if (booleano(datos, "sangradoLeve")) {
            reasonsAmarillo.add("Sangrado leve que requiere curacion.");
        }

        // Constantes vitales.
        Double temperatura = decimal(datos, "temperatura");
        if (temperatura != null) {
            if (temperatura < 35 || temperatura > 41) {
                reasonsRojo.add("Temperatura fuera de rango critico (" + temperatura + " C).");
            } else if (temperatura < 36 || temperatura > 39.5) {
                reasonsAmarillo.add("Temperatura alterada (" + temperatura + " C).");
            }
        }

        Double cardiaca = decimal(datos, "frecuenciaCardiaca");
        if (cardiaca != null) {
            if (cardiaca < 40 || cardiaca > 250) {
                reasonsRojo.add("Frecuencia cardiaca fuera de rango critico (" + cardiaca + " lpm).");
            } else if (cardiaca < 60 || cardiaca > 220) {
                reasonsAmarillo.add("Frecuencia cardiaca alterada (" + cardiaca + " lpm).");
            }
        }

        Double respiratoria = decimal(datos, "frecuenciaRespiratoria");
        if (respiratoria != null) {
            if (respiratoria < 10 || respiratoria > 45) {
                reasonsRojo.add("Frecuencia respiratoria fuera de rango critico (" + respiratoria + " rpm).");
            } else if (respiratoria < 15 || respiratoria > 35) {
                reasonsAmarillo.add("Frecuencia respiratoria alterada (" + respiratoria + " rpm).");
            }
        }

        // Nivel del paciente (fisiologia) y dolor.
        String estado = texto(datos, "estado");
        if ("critico".equals(estado)) {
            reasonsRojo.add("El paciente fue registrado como critico.");
        } else if ("grave".equals(estado)) {
            reasonsNaranja.add("Estado clinico grave registrado.");
        } else if ("moderado".equals(estado)) {
            reasonsAmarillo.add("Estado clinico moderado registrado.");
        }

        Integer dolor = entero(datos, "dolor");
        if (dolor != null) {
            if (dolor >= 8) {
                reasonsRojo.add("Dolor intenso (" + dolor + "/10).");
            } else if (dolor >= 4) {
                reasonsNaranja.add("Dolor importante (" + dolor + "/10).");
            }
        }

        if (booleano(datos, "vomitos") || booleano(datos, "diarrea")) {
            reasonsAmarillo.add("Sintomas gastrointestinales: descartar deshidratacion.");
        }

        Nivel nivel = Nivel.VERDE;
        List<String> motivos = new ArrayList<>();
        if (!reasonsRojo.isEmpty()) {
            nivel = Nivel.ROJO;
            motivos.addAll(reasonsRojo);
        } else if (!reasonsNaranja.isEmpty()) {
            nivel = Nivel.NARANJA;
            motivos.addAll(reasonsNaranja);
        } else if (!reasonsAmarillo.isEmpty()) {
            nivel = Nivel.AMARILLO;
            motivos.addAll(reasonsAmarillo);
        }

        if (motivos.isEmpty()) {
            motivos.add("No se detectaron signos de alarma con los datos recibidos.");
        }

        return new Resultado(nivel, motivos, siguientePaso(nivel));
    }

    /** Respuesta lista para enviar al frontend. */
    public Map<String, Object> detalle(Map<String, Object> signos) {
        Resultado resultado = clasificar(signos);
        Map<String, Object> mapa = new LinkedHashMap<>();
        mapa.put("nivel", resultado.nivel().name());
        mapa.put("etiqueta", resultado.nivel().getEtiqueta());
        mapa.put("descripcion", resultado.nivel().getDescripcion());
        mapa.put("motivos", resultado.motivos());
        mapa.put("siguientePaso", resultado.siguientePaso());
        return mapa;
    }

    private String siguientePaso(Nivel nivel) {
        return switch (nivel) {
            case ROJO -> "Activacion de la guardia e estabilizacion inmediata.";
            case NARANJA -> "Valoracion medica en los siguientes 15 minutos.";
            case AMARILLO -> "Consulta clinica en la siguiente hora disponible.";
            case VERDE -> "Asignar veterinario de guardia y continuar en el orden de llegada.";
        };
    }

    /** Normaliza claves y valores para comparar sin depender de tildes, espacios ni mayusculas. */
    private Map<String, Object> normalizar(Map<String, Object> signos) {
        Map<String, Object> datos = new LinkedHashMap<>();
        if (signos == null) {
            return datos;
        }
        signos.forEach((clave, valor) -> {
            if (clave == null) {
                return;
            }
            datos.put(normalizarClave(clave), valor);
        });
        return datos;
    }

    /**
     * Deja la clave en minusculas, sin tildes y sin espacios, guiones o
     * guiones bajos: "Frecuencia Respiratoria" y "frecuenciaRespiratoria"
     * terminan siendo la misma clave.
     */
    private String normalizarClave(String clave) {
        return clave.trim().toLowerCase(Locale.ROOT)
                .replaceAll("[\\u00C0-\\u00FF]", "")
                .replaceAll("[\\s_-]", "");
    }

    private boolean booleano(Map<String, Object> datos, String clave) {
        Object valor = datos.get(normalizarClave(clave));
        if (valor instanceof Boolean booleano) {
            return booleano;
        }
        if (valor instanceof String texto) {
            return "true".equalsIgnoreCase(texto.trim()) || "si".equalsIgnoreCase(texto.trim())
                    || "sí".equalsIgnoreCase(texto.trim());
        }
        return false;
    }

    private Double decimal(Map<String, Object> datos, String clave) {
        Object valor = datos.get(normalizarClave(clave));
        if (valor instanceof Number numero) {
            return numero.doubleValue();
        }
        if (valor instanceof String texto && !texto.isBlank()) {
            try {
                return Double.parseDouble(texto.trim().replace(',', '.'));
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    private Integer entero(Map<String, Object> datos, String clave) {
        Double valor = decimal(datos, clave);
        return valor == null ? null : (int) Math.round(valor);
    }

    private String texto(Map<String, Object> datos, String clave) {
        Object valor = datos.get(normalizarClave(clave));
        return valor == null ? "" : normalizarValor(String.valueOf(valor));
    }

    /** Normaliza el valor recibido: minusculas y sin tildes ("critico" = "crítico"). */
    private String normalizarValor(String valor) {
        return valor.trim().toLowerCase(Locale.ROOT).replaceAll("[\\u00C0-\\u00FF]", "");
    }
}
