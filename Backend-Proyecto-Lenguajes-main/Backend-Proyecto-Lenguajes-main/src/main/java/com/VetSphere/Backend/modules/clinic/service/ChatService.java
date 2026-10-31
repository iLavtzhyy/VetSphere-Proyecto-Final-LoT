package com.VetSphere.Backend.modules.clinic.service;

import com.VetSphere.Backend.modules.clinic.domain.Cita;
import com.VetSphere.Backend.modules.clinic.domain.Factura;
import com.VetSphere.Backend.modules.clinic.domain.HistorialClinico;
import com.VetSphere.Backend.modules.clinic.domain.Mascota;
import com.VetSphere.Backend.modules.clinic.domain.Producto;
import com.VetSphere.Backend.modules.clinic.domain.Usuario;
import com.VetSphere.Backend.modules.clinic.dto.ChatResponse;
import com.VetSphere.Backend.modules.clinic.dto.MensajeChat;
import com.VetSphere.Backend.modules.clinic.repository.CitaRepository;
import com.VetSphere.Backend.modules.clinic.repository.FacturaRepository;
import com.VetSphere.Backend.modules.clinic.repository.MascotaRepository;
import com.VetSphere.Backend.modules.clinic.repository.ProductoRepository;
import com.VetSphere.Backend.modules.clinic.repository.UsuarioRepository;
import com.VetSphere.Backend.modules.clinic.service.CuponService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.ResourceAccessException;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Asistente inteligente de VetSphere basado en la API de OpenAI
 * (compatible con cualquier proveedor tipo chat/completions: OpenAI, Azure,
 * proxies locales, etc.). El modelo puede llamar a herramientas de solo
 * lectura contra la base de datos del sistema; nunca se le confian escrituras.
 */
@Slf4j
@Service
public class ChatService {

    /** Numero maximo de idas y vueltas con herramientas por pregunta. */
    private static final int MAX_ITERACIONES_HERRAMIENTAS = 3;
    private static final int LIMITE_RESULTADOS = 5;
    private static final int LIMITE_FACTURAS = 10;
    /** Se conserva el historial completo en BD; el contexto del modelo usa un tramo amplio reciente. */
    private static final int MAX_MENSAJES_HISTORIAL = 24;
    private static final int MAX_CARACTERES_HISTORIAL = 8000;

    private static final String INSTRUCCION_SISTEMA = """
            Eres el asistente inteligente de VetSphere, el sistema de gestion de la clinica
            veterinaria. Respondes siempre en espanol, con un tono amable y profesional.
            Solo respondes con la informacion que conoces o la que obtienes mediante las
            herramientas disponibles; si no tienes los datos, dilo claramente y no inventes.
            Cuando te pregunten por mascotas, clientes, historiales, stock de la farmacia,
            citas, facturas o cupones/promociones, usa la herramienta correspondiente antes de responder.
            Interpreta español cotidiano, sinónimos, errores tipográficos comunes y preguntas compuestas
            (por ejemplo: "qué tengo hoy y mañana", "qué medicamentos se están acabando", "cómo está mi mascota").
            Si una pregunta contiene varios pedidos independientes, atiéndelos todos en orden. Si falta un dato
            para identificar un registro, pregunta cuál dato necesitas en lugar de decir que no existe.
            Los datos se consultan de la base de datos actual en cada pregunta; nunca afirmes que
            una operacion se ejecuto. Para crear un cupon, indica que el usuario debe confirmarlo
            mediante el flujo guiado del chat. No inventes resultados ni datos.
            Da respuestas breves y concretas (listas cortas cuando aplique). No reveles
            contrasenas, codigos de verificacion ni datos internos de configuracion.
            """;

    private static final String RUTA_CHAT_COMPLETIONS = "/chat/completions";

    private final RestClient restClient;
    private final ObjectMapper mapper;
    private final String modelo;
    private final String apiKey;
    private final List<Map<String, Object>> herramientas;

    private final MascotaRepository mascotaRepository;
    private final UsuarioRepository usuarioRepository;
    private final ProductoRepository productoRepository;
    private final CitaRepository citaRepository;
    private final FacturaRepository facturaRepository;
    private final MascotaService mascotaService;
    private final CuponService cuponService;

    public ChatService(RestClient.Builder builder,
                       ObjectMapper mapper,
                       MascotaRepository mascotaRepository,
                       UsuarioRepository usuarioRepository,
                       ProductoRepository productoRepository,
                       CitaRepository citaRepository,
                       FacturaRepository facturaRepository,
                       MascotaService mascotaService,
                       CuponService cuponService,
                       @Value("${app.chat.base-url:https://api.openai.com/v1}") String baseUrl,
                       @Value("${app.chat.api-key:}") String apiKey,
                       @Value("${app.chat.modelo:gpt-4o-mini}") String modelo) {
        this.mapper = mapper;
        this.modelo = modelo;
        this.apiKey = apiKey;
        this.mascotaRepository = mascotaRepository;
        this.usuarioRepository = usuarioRepository;
        this.productoRepository = productoRepository;
        this.citaRepository = citaRepository;
        this.facturaRepository = facturaRepository;
        this.mascotaService = mascotaService;
        this.cuponService = cuponService;
        this.herramientas = definirHerramientas();

        SimpleClientHttpRequestFactory fabricaPeticiones = new SimpleClientHttpRequestFactory();
        fabricaPeticiones.setConnectTimeout(Duration.ofSeconds(10));
        fabricaPeticiones.setReadTimeout(Duration.ofSeconds(60));
        this.restClient = builder
                .baseUrl(normalizarBaseUrl(baseUrl))
                .requestFactory(fabricaPeticiones)
                .build();
    }

    /** Evita el doble slash cuando la base-url termina en "/" (p. ej. proxies locales). */
    private static String normalizarBaseUrl(String baseUrl) {
        String url = baseUrl == null ? "" : baseUrl.trim();
        return url.replaceAll("/+$", "");
    }

    /**
     * Procesa una pregunta del usuario. Todos los fallos (sin clave de API,
     * errores de red, respuestas inesperadas del proveedor) se traducen en un
     * mensaje amigable: el chat nunca revienta la peticion HTTP.
     */
    public ChatResponse preguntar(String pregunta) {
        return preguntar(pregunta, null);
    }

    /**
     * Procesa una pregunta del usuario con su historial de conversacion
     * opcional (memoria de corto plazo que envia el frontend). Todos los
     * fallos (sin clave de API, errores de red, respuestas inesperadas del
     * proveedor) se traducen en un mensaje amigable: el chat nunca revienta
     * la peticion HTTP.
     */
    public ChatResponse preguntar(String pregunta, List<MensajeChat> historial) {
        // Preguntas de inventario ambiguas como "cuáles están bajo stock" son
        // más fiables con una consulta directa que delegando la intención al LLM.
        if (esConsultaStockBajo(normalizarTexto(pregunta))) {
            return responderModoLocal(pregunta);
        }
        if (!StringUtils.hasText(apiKey)) {
            return responderModoLocal(pregunta);
        }
        boolean consultoBaseDatos = false;
        try {
            List<Map<String, Object>> mensajes = new ArrayList<>();
            mensajes.add(Map.of("role", "system", "content", instruccionesParaRol()));
            mensajes.addAll(sanitizarHistorial(historial));
            mensajes.add(Map.of("role", "user", "content", pregunta));

            for (int iteracion = 0; iteracion < MAX_ITERACIONES_HERRAMIENTAS; iteracion++) {
                JsonNode mensaje = invocarModelo(mensajes, herramientasPermitidas()).path("choices").path(0).path("message");
                JsonNode llamadas = mensaje.path("tool_calls");
                JsonNode nodoContenido = mensaje.path("content");
                // isTextual evita el caso NullNode, cuyo asText devuelve el literal "null"
                String contenido = nodoContenido.isTextual() ? nodoContenido.asText("").trim() : "";

                if (contenido.isEmpty() && (llamadas.isMissingNode() || llamadas.isEmpty())) {
                    return new ChatResponse("No obtuve una respuesta del asistente. Intenta reformular la pregunta.",
                            consultoBaseDatos, true);
                }
                if (llamadas.isMissingNode() || llamadas.isEmpty()) {
                    return new ChatResponse(contenido, consultoBaseDatos, true);
                }

                // El modelo pide ejecutar herramientas: se ejecutan y se le devuelven los resultados.
                consultoBaseDatos = true;
                mensajes.add(construirMensajeAsistente(contenido, llamadas));
                for (JsonNode llamada : llamadas) {
                    Map<String, Object> mensajeResultado = new LinkedHashMap<>();
                    mensajeResultado.put("role", "tool");
                    mensajeResultado.put("tool_call_id", llamada.path("id").asText(""));
                    mensajeResultado.put("content", ejecutarHerramienta(llamada));
                    mensajes.add(mensajeResultado);
                }
            }
            return new ChatResponse(
                    "Realice demasiadas consultas para responder esa pregunta. "
                            + "Podrias simplificarla o pedirme un dato mas concreto?",
                    consultoBaseDatos, true);
        } catch (RestClientResponseException ex) {
            int estado = ex.getStatusCode().value();
            log.warn("El proveedor del asistente respondió con HTTP {}", estado);
            if (estado == 402) return responderProveedorSinSaldo(pregunta);
            return responderModoLocal(pregunta);
        } catch (ResourceAccessException ex) {
            log.warn("No hay conexión con el proveedor del asistente: {}", ex.getClass().getSimpleName());
            return responderModoLocal(pregunta);
        } catch (Exception ex) {
            log.warn("Fallo la invocacion al asistente inteligente: {}", ex.getClass().getSimpleName());
            return responderModoLocal(pregunta);
        }
    }

    private ChatResponse responderProveedorSinSaldo(String pregunta) {
        ChatResponse respuestaLocal = responderModoLocal(pregunta);
        if (!respuestaLocal.respuesta().startsWith("Puedo atender consultas frecuentes")) {
            return respuestaLocal;
        }
        return new ChatResponse("La API de IA está conectada, pero el proveedor rechazó la consulta por un problema de facturación o saldo. "
                + "Las consultas rápidas de VetSphere siguen disponibles; cuando se restablezca el servicio podré responder preguntas abiertas.",
                false, true);
    }

    /** Respuestas locales para mantener útiles las consultas comunes si el proveedor IA no responde. */
    private ChatResponse responderModoLocal(String pregunta) {
        String texto = pregunta == null ? "" : pregunta.trim();
        String normalizado = normalizarTexto(texto);
        if (esConsultaCita(normalizado) && normalizado.contains("hoy") && normalizado.contains("manana")) {
            return new ChatResponse("Citas de hoy: " + resumenCitas(LocalDate.now())
                    + "\n\nCitas de mañana: " + resumenCitas(LocalDate.now().plusDays(1)), true, true);
        }
        ChatResponse compuesta = responderPreguntasCompuestas(texto);
        if (compuesta != null) return compuesta;
        if (normalizado.matches("^(hola|buenos dias|buenas tardes|buenas noches|hey|buenas)[!. ]*$")) {
            return new ChatResponse("¡Hola! " + capacidadesRol() + " Escribe ‘ayuda’ para ver ejemplos.", false, true);
        }
        if (normalizado.matches("^(ayuda|que puedes hacer|que puedo consultar|mis permisos|opciones|que sabes hacer|como me ayudas)\\??$")) {
            return new ChatResponse(capacidadesRol(), false, true);
        }
        if (esConsultaCliente(normalizado)) {
            if (!tieneRol("ROLE_ADMIN", "ROLE_RECEPCIONISTA")) {
                return new ChatResponse("Tu rol no puede consultar datos de clientes. " + capacidadesRol(), false, true);
            }
            String terminos = limpiarTerminos(texto, "cliente", "clientes", "propietario", "propietarios", "buscar", "busca", "dime", "muestra", "mostrar", "encuentra", "datos", "informacion", "telefono", "correo", "email", "de", "del", "la", "el", "por", "favor", "quien", "cual", "es", "son", "me", "puedes", "hay");
            List<?> clientes = (List<?>) buscarCliente(mapearArgumentos(Map.of("texto", terminos))).get("clientes");
            return new ChatResponse(clientes == null || clientes.isEmpty()
                    ? "No encontré clientes con esos datos. Prueba con su nombre, apellido, correo o documento."
                    : "Clientes encontrados: " + resumirLista(clientes, "nombres", "apellidos", "email", "telefono"), true, true);
        }
        if (esConsultaFactura(normalizado)) {
            if (!tieneRol("ROLE_ADMIN", "ROLE_RECEPCIONISTA")) {
                return new ChatResponse("Tu rol no puede consultar facturas desde el chat. " + capacidadesRol(), false, true);
            }
            String email = extraerCorreo(texto);
            Map<String, Object> clienteEncontrado = null;
            if (email.isBlank()) {
                String terminos = limpiarTerminos(texto, "factura", "facturas", "comprobante", "comprobantes", "cliente", "clientes", "historial", "ver", "buscar", "busca", "dime", "muestra", "mostrar", "de", "del", "la", "el", "por", "favor", "cual", "son", "sus", "tiene", "tienen");
                List<?> clientes = (List<?>) buscarCliente(mapearArgumentos(Map.of("texto", terminos))).get("clientes");
                if (clientes != null && !clientes.isEmpty() && clientes.get(0) instanceof Map<?, ?> mapa) {
                    clienteEncontrado = (Map<String, Object>) mapa;
                }
            }
            Map<String, Object> facturas = email.isBlank()
                    ? clienteEncontrado == null ? Map.of("error", "Falta identificar al cliente")
                        : facturasCliente(mapearArgumentos(Map.of("clienteId", clienteEncontrado.get("id"))))
                    : facturasCliente(mapearArgumentos(Map.of("email", email)));
            if (facturas.containsKey("error")) {
                return new ChatResponse("Para buscar facturas necesito el correo del cliente o un nombre y apellido que lo identifiquen.", false, true);
            }
            List<?> lista = (List<?>) facturas.get("facturas");
            return new ChatResponse(lista == null || lista.isEmpty() ? "Ese cliente no tiene facturas registradas." : "Facturas encontradas: " + resumirLista(lista, "numero", "fecha", "concepto", "total", "estadoPago"), true, true);
        }
        if (normalizado.contains("cupon") || normalizado.contains("promocion")) {
            if (!tieneRol("ROLE_ADMIN", "ROLE_RECEPCIONISTA")) {
                return new ChatResponse("Tu rol no puede consultar cupones. " + capacidadesRol(), false, true);
            }
            Map<String, Object> datos = normalizado.contains("vigente") || normalizado.contains("actual")
                    ? cuponesVigentes() : buscarCupon(mapearArgumentos(Map.of("texto", texto)));
            List<?> lista = (List<?>) datos.get("cupones");
            if (lista == null || lista.isEmpty()) return new ChatResponse("No encontré cupones para esa consulta.", true, true);
            return new ChatResponse("Cupones encontrados: " + resumirLista(lista, "codigo", "descripcion", "valor", "tipoDescuento"), true, true);
        }
        if (normalizado.contains("mascota") || normalizado.contains("historial") || normalizado.contains("paciente")) {
            Map<String, Object> datos = buscarMascota(mapearArgumentos(Map.of("texto", limpiarTerminos(texto, "mascota", "mascotas", "paciente", "pacientes", "historial", "clinico", "clínico", "ver", "buscar", "busca", "dime", "muestra", "mostrar", "como", "esta", "tiene", "de", "del", "la", "el", "por", "favor", "mi", "mis", "su", "sus", "que", "cual", "es", "me", "puedes"))));
            List<?> lista = (List<?>) datos.get("mascotas");
            if (lista == null || lista.isEmpty()) return new ChatResponse("No encontré mascotas con ese nombre en la información que tu rol puede consultar.", true, true);
            if (normalizado.contains("historial")) {
                Object primero = lista.get(0);
                if (primero instanceof Map<?, ?> mascota && mascota.get("id") instanceof Number id) {
                    Map<String, Object> historial = historialMascota(mapearArgumentos(Map.of("mascotaId", id.longValue())));
                    List<?> eventos = (List<?>) historial.get("historial");
                    return new ChatResponse(eventos == null || eventos.isEmpty() ? "La mascota no tiene eventos clínicos registrados." : "Historial reciente: " + resumirLista(eventos, "fecha", "tipoEvento", "diagnostico", "tratamiento"), true, true);
                }
            }
            return new ChatResponse("Mascotas encontradas: " + resumirLista(lista, "nombre", "especie", "raza", "estado"), true, true);
        }
        if (esConsultaCita(normalizado)) {
            LocalDate fecha = normalizado.contains("manana") || normalizado.contains("proxima") || normalizado.contains("siguiente")
                    ? LocalDate.now().plusDays(1) : normalizado.contains("pasado manana")
                    ? LocalDate.now().plusDays(2) : normalizado.contains("ayer") ? LocalDate.now().minusDays(1) : LocalDate.now();
            return new ChatResponse(resumenCitas(fecha), true, true);
        }
        if (esConsultaFarmacia(normalizado)) {
            if (!tieneRol("ROLE_ADMIN", "ROLE_RECEPCIONISTA")) return new ChatResponse("Tu rol no tiene acceso al inventario de farmacia. " + capacidadesRol(), false, true);
            if (esConsultaStockBajo(normalizado)) return responderProductosBajoStock();
            String terminos = limpiarTerminos(texto, "stock", "farmacia", "inventario", "producto", "productos", "medicamento", "medicamentos", "disponible", "disponibles", "cuanto", "cuantos", "cuantas", "hay", "tenemos", "queda", "quedan", "dime", "muestra", "mostrar", "busca", "buscar", "de", "del", "la", "el", "por", "favor", "que", "cual", "es", "son");
            Map<String, Object> datos = stockFarmacia(mapearArgumentos(Map.of("texto", terminos)));
            List<?> lista = (List<?>) datos.get("productos");
            return new ChatResponse(lista == null || lista.isEmpty() ? "No encontré productos para esa búsqueda." : "Productos encontrados: " + resumirLista(lista, "nombre", "codigo", "stock", "enRiesgo"), true, true);
        }
        return new ChatResponse("Puedo atender consultas frecuentes con respuestas rápidas. " + capacidadesRol()
                + " Para otras preguntas, el asistente conversacional avanzado no está disponible temporalmente.", false, true);
    }

    private ChatResponse responderProductosBajoStock() {
        if (!tieneRol("ROLE_ADMIN", "ROLE_RECEPCIONISTA")) {
            return new ChatResponse("Tu rol no tiene acceso al inventario de farmacia. " + capacidadesRol(), false, true);
        }
        List<Producto> productos = productoRepository.findAll().stream()
                .filter(producto -> producto.getStock() != null && producto.getStockCritico() != null
                        && producto.getStock() <= producto.getStockCritico())
                .sorted(Comparator.comparing(Producto::getStock))
                .toList();
        if (productos.isEmpty()) {
            return new ChatResponse("No hay productos bajo el nivel mínimo de stock. El inventario está dentro de los límites configurados.", true, true);
        }
        int mostrados = Math.min(productos.size(), 10);
        String detalle = productos.subList(0, mostrados).stream()
                .map(producto -> producto.getNombre() + " (" + producto.getStock() + " disponibles; mínimo "
                        + producto.getStockCritico() + ")")
                .collect(java.util.stream.Collectors.joining("; "));
        String adicional = productos.size() > mostrados ? "; y " + (productos.size() - mostrados) + " más" : "";
        return new ChatResponse("Encontré " + productos.size() + " productos bajo stock: " + detalle + adicional + ".", true, true);
    }

    private static boolean esConsultaStockBajo(String textoNormalizado) {
        return textoNormalizado.contains("bajo stock") || textoNormalizado.contains("stock bajo")
                || textoNormalizado.contains("stock critico") || textoNormalizado.contains("stock minimo")
                || textoNormalizado.contains("bajo de stock") || textoNormalizado.contains("por debajo del minimo")
                || textoNormalizado.contains("por debajo del nivel minimo") || textoNormalizado.contains("por agotar")
                || textoNormalizado.contains("agotandose") || textoNormalizado.contains("productos criticos")
                || textoNormalizado.contains("se esta acabando") || textoNormalizado.contains("se estan acabando")
                || textoNormalizado.contains("quedan pocos") || textoNormalizado.contains("casi agotado")
                || textoNormalizado.contains("casi agotados") || textoNormalizado.contains("necesita reposicion");
    }

    private ChatResponse responderPreguntasCompuestas(String texto) {
        Map<String, String> porIntencion = new LinkedHashMap<>();
        for (String parte : texto.split("(?i)\\s+(?:y tambien|tambien|ademas|y)\\s+|[,;?]+")) {
            String normalizado = normalizarTexto(parte);
            String intencion = clasificarIntencion(normalizado);
            if (intencion != null && !normalizado.isBlank()) porIntencion.putIfAbsent(intencion, parte.trim());
        }
        if (porIntencion.size() < 2) return null;
        List<String> respuestas = porIntencion.values().stream()
                .map(parte -> responderModoLocal(parte).respuesta())
                .toList();
        return new ChatResponse(String.join("\n\n", respuestas), true, true);
    }

    /** Clasifica peticiones en lenguaje cotidiano para responder varias en un mismo turno. */
    private static String clasificarIntencion(String texto) {
        if (esConsultaStockBajo(texto) || esConsultaFarmacia(texto)) return "farmacia";
        if (texto.contains("cupon") || texto.contains("promocion")) return "cupones";
        if (esConsultaFactura(texto)) return "facturas";
        if (esConsultaCita(texto)) return "citas";
        if (esConsultaCliente(texto)) return "clientes";
        if (texto.contains("mascota") || texto.contains("paciente") || texto.contains("historial")) return "mascotas";
        return null;
    }

    private static boolean esConsultaFarmacia(String texto) {
        return texto.contains("stock") || texto.contains("farmacia") || texto.contains("inventario")
                || texto.contains("medicamento") || texto.contains("medicina") || texto.contains("producto");
    }

    private static boolean esConsultaCita(String texto) {
        return texto.contains("cita") || texto.contains("agenda") || texto.contains("turno")
                || texto.contains("consulta") || texto.contains("atencion medica");
    }

    private static boolean esConsultaCliente(String texto) {
        return (texto.contains("cliente") || texto.contains("propietario") || texto.contains("correo del usuario"))
                && !esConsultaFactura(texto);
    }

    private static boolean esConsultaFactura(String texto) {
        return texto.contains("factura") || texto.contains("facturas") || texto.contains("comprobante");
    }

    private static String limpiarTerminos(String texto, String... palabras) {
        String resultado = normalizarTexto(texto);
        for (String palabra : palabras) resultado = resultado.replaceAll("\\b" + java.util.regex.Pattern.quote(normalizarTexto(palabra)) + "\\b", " ");
        return resultado.replaceAll("[^\\p{L}\\p{N}@._-]+", " ").replaceAll("\\s+", " ").trim();
    }

    private static String extraerCorreo(String texto) {
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}", java.util.regex.Pattern.CASE_INSENSITIVE)
                .matcher(texto == null ? "" : texto);
        return matcher.find() ? matcher.group() : "";
    }

    private String resumenCitas(LocalDate fecha) {
        Map<String, Object> datos = citasFecha(mapearArgumentos(Map.of("fecha", fecha.toString())));
        List<?> lista = (List<?>) datos.get("citas");
        return lista == null || lista.isEmpty() ? "No hay citas registradas para " + fecha + "."
                : "Citas del " + fecha + ": " + resumirLista(lista, "fechaHora", "mascota", "motivo", "estado");
    }

    private static String normalizarTexto(String texto) {
        return Normalizer.normalize(texto == null ? "" : texto.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").replaceAll("\\s+", " ").trim();
    }

    private String capacidadesRol() {
        if (tieneRol("ROLE_ADMIN")) return "Como administrador puedo consultar mascotas, historiales, citas, clientes, farmacia, facturas y cupones; crear cupones requiere confirmación en el flujo guiado.";
        if (tieneRol("ROLE_RECEPCIONISTA")) return "Como recepcionista puedo consultar mascotas, historiales, citas, clientes, farmacia, facturas y cupones vigentes. No puedo modificar datos desde el chat.";
        if (tieneRol("ROLE_VETERINARIO")) return "Como veterinario puedo consultar mascotas, historiales clínicos y citas.";
        if (tieneRol("ROLE_CLIENTE")) return "Como cliente puedo consultar únicamente tus mascotas, sus historiales y tus citas.";
        return "No hay consultas habilitadas para este rol.";
    }

    private JsonNode mapearArgumentos(Map<String, ?> argumentos) {
        return mapper.valueToTree(argumentos);
    }

    private String resumirLista(List<?> elementos, String... campos) {
        return elementos.stream().limit(5).map(item -> {
            if (!(item instanceof Map<?, ?> mapa)) return String.valueOf(item);
            return java.util.Arrays.stream(campos).filter(k -> mapa.get(k) != null && !String.valueOf(mapa.get(k)).isBlank())
                    .map(k -> k + ": " + mapa.get(k)).collect(java.util.stream.Collectors.joining(" · "));
        }).filter(s -> !s.isBlank()).collect(java.util.stream.Collectors.joining("; "));
    }

    // ------------------------------------------------------------------
    // Historial de conversacion (memoria de corto plazo)
    // ------------------------------------------------------------------

    /**
     * Convierte el historial que envia el cliente en mensajes validos para el
     * modelo, entre el mensaje de sistema y la pregunta actual:
     * - solo roles "user" y "assistant" (el resto se descarta en silencio,
     *   de modo que el historial nunca puede inyectar instrucciones de sistema);
     * - sin nulos, recortados y sin contenido vacio;
     * - se conserva un contexto amplio reciente para respetar el limite del proveedor IA.
     */
    private List<Map<String, Object>> sanitizarHistorial(List<MensajeChat> historial) {
        if (historial == null || historial.isEmpty()) {
            return List.of();
        }
        List<Map<String, Object>> mensajes = new ArrayList<>();
        for (MensajeChat mensaje : historial) {
            if (mensaje == null) {
                continue;
            }
            String rol = mensaje.rol() == null ? "" : mensaje.rol().trim().toLowerCase(Locale.ROOT);
            if (!"user".equals(rol) && !"assistant".equals(rol)) {
                continue;
            }
            String contenido = mensaje.contenido() == null ? "" : mensaje.contenido().trim();
            if (contenido.isEmpty()) {
                continue;
            }
            if (contenido.length() > MAX_CARACTERES_HISTORIAL) {
                contenido = contenido.substring(0, MAX_CARACTERES_HISTORIAL);
            }
            Map<String, Object> mensajeLimpio = new LinkedHashMap<>();
            mensajeLimpio.put("role", rol);
            mensajeLimpio.put("content", contenido);
            mensajes.add(mensajeLimpio);
        }
        if (mensajes.size() > MAX_MENSAJES_HISTORIAL) {
            return new ArrayList<>(mensajes.subList(mensajes.size() - MAX_MENSAJES_HISTORIAL, mensajes.size()));
        }
        return mensajes;
    }

    // ------------------------------------------------------------------
    // Proveedor de IA
    // ------------------------------------------------------------------

    private JsonNode invocarModelo(List<Map<String, Object>> mensajes, List<Map<String, Object>> herramientasRol) {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("model", modelo);
        cuerpo.put("messages", mensajes);
        cuerpo.put("tools", herramientasRol);
        cuerpo.put("temperature", 0.2);

        JsonNode respuesta = restClient.post()
                .uri(RUTA_CHAT_COMPLETIONS)
                .header("Authorization", "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(cuerpo)
                .retrieve()
                .body(JsonNode.class);
        if (respuesta == null) {
            throw new IllegalStateException("El proveedor de IA devolvio una respuesta vacia");
        }
        return respuesta;
    }

    private Map<String, Object> construirMensajeAsistente(String contenido, JsonNode llamadas) {
        Map<String, Object> mensaje = new LinkedHashMap<>();
        mensaje.put("role", "assistant");
        if (!contenido.isEmpty()) {
            mensaje.put("content", contenido);
        }
        mensaje.put("tool_calls", mapper.convertValue(llamadas, new TypeReference<List<Map<String, Object>>>() {
        }));
        return mensaje;
    }

    // ------------------------------------------------------------------
    // Ejecucion de herramientas
    // ------------------------------------------------------------------

    private String ejecutarHerramienta(JsonNode llamada) {
        String nombre = llamada.path("function").path("name").asText("");
        JsonNode argumentos = leerArgumentos(llamada.path("function").path("arguments"));
        try {
            if (!herramientasPermitidas().stream().anyMatch(h -> nombre.equals(
                    String.valueOf(((Map<?, ?>) h.get("function")).get("name"))))) {
                return "{\"error\":\"Esta consulta no está disponible para tu rol.\"}";
            }
            Map<String, Object> resultado = switch (nombre) {
                case "buscar_mascota" -> buscarMascota(argumentos);
                case "buscar_cliente" -> buscarCliente(argumentos);
                case "historial_mascota" -> historialMascota(argumentos);
                case "stock_farmacia" -> stockFarmacia(argumentos);
                case "citas_fecha" -> citasFecha(argumentos);
                case "facturas_cliente" -> facturasCliente(argumentos);
                case "cupones_vigentes" -> cuponesVigentes();
                case "buscar_cupon" -> buscarCupon(argumentos);
                default -> Map.of("error", "Herramienta desconocida: " + nombre);
            };
            return mapper.writeValueAsString(resultado);
        } catch (Exception ex) {
            log.warn("Fallo la herramienta '{}' del asistente: {}", nombre, ex.getMessage());
            return "{\"error\":\"No se pudo ejecutar la herramienta " + nombre + "\"}";
        }
    }

    private List<Map<String, Object>> herramientasPermitidas() {
        boolean admin = tieneRol("ROLE_ADMIN");
        boolean recepcion = tieneRol("ROLE_RECEPCIONISTA");
        boolean personalClinico = tieneRol("ROLE_VETERINARIO");
        boolean cliente = tieneRol("ROLE_CLIENTE");
        List<String> permitidas = new ArrayList<>(List.of("buscar_mascota", "historial_mascota", "citas_fecha"));
        if (admin || recepcion) {
            permitidas.addAll(List.of("buscar_cliente", "stock_farmacia", "facturas_cliente", "cupones_vigentes", "buscar_cupon"));
        }
        // Denegar por defecto: cualquier autoridad desconocida conserva solo las consultas clínicas básicas.
        if (!(admin || recepcion || personalClinico || cliente)) return List.of();
        return herramientas.stream().filter(h -> {
            Object function = h.get("function");
            return function instanceof Map<?, ?> map && permitidas.contains(String.valueOf(map.get("name")));
        }).toList();
    }

    private String instruccionesParaRol() {
        String capacidades;
        if (tieneRol("ROLE_ADMIN")) {
            capacidades = "Puedes consultar mascotas, historiales, citas, clientes, farmacia, facturas y cupones vigentes. "
                    + "La creación de cupones solo se realiza en el flujo guiado y requiere confirmación explícita del administrador.";
        } else if (tieneRol("ROLE_RECEPCIONISTA")) {
            capacidades = "Puedes consultar mascotas, historiales, citas, clientes, inventario de farmacia, facturas y cupones vigentes. "
                    + "No puedes crear cupones ni cambiar datos desde el asistente.";
        } else if (tieneRol("ROLE_VETERINARIO")) {
            capacidades = "Puedes consultar mascotas, historiales clínicos y citas. No puedes consultar información administrativa, financiera, clientes, farmacia ni cupones desde el asistente.";
        } else if (tieneRol("ROLE_CLIENTE")) {
            capacidades = "Solo puedes consultar tus propias mascotas, sus historiales y tus citas. No puedes consultar datos de otras personas, inventario, facturas ni cupones.";
        } else {
            capacidades = "No hay consultas habilitadas para tu rol.";
        }
        return INSTRUCCION_SISTEMA + "\n\nPermisos de esta sesión: " + capacidades
                + " Responde solo dentro de estos permisos; si piden otra acción, explica brevemente qué consultas sí están disponibles. "
                + "No inventes permisos ni afirmes haber realizado cambios.";
    }

    private boolean esClienteSinPermisosInternos() {
        return tieneRol("ROLE_CLIENTE") && !tieneRol("ROLE_ADMIN", "ROLE_RECEPCIONISTA", "ROLE_VETERINARIO");
    }

    private boolean esMascotaDelUsuario(Mascota mascota) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && mascota.getPropietario() != null
                && mascota.getPropietario().getEmail() != null
                && mascota.getPropietario().getEmail().equalsIgnoreCase(auth.getName());
    }

    private boolean tieneRol(String... roles) {
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacion == null || !autenticacion.isAuthenticated()) return false;
        return autenticacion.getAuthorities().stream()
                .anyMatch(authority -> List.of(roles).contains(authority.getAuthority()));
    }

    private JsonNode leerArgumentos(JsonNode nodo) {
        String texto = nodo.path("text").asText("").trim();
        if (!StringUtils.hasText(texto)) {
            return mapper.createObjectNode();
        }
        try {
            return mapper.readTree(texto);
        } catch (Exception ex) {
            return mapper.createObjectNode();
        }
    }

    private Map<String, Object> buscarMascota(JsonNode argumentos) {
        String texto = argumentos.path("texto").asText("").trim().toLowerCase(Locale.ROOT);
        List<Mascota> mascotasVisibles = esClienteSinPermisosInternos()
                ? mascotaRepository.findByPropietarioEmail(SecurityContextHolder.getContext().getAuthentication().getName())
                : mascotaRepository.findAll();
        List<Map<String, Object>> mascotas = mascotasVisibles.stream()
                .filter(mascota -> coincide(texto, mascota.getNombre(), mascota.getEspecie(), mascota.getRaza()))
                .limit(LIMITE_RESULTADOS)
                .map(this::resumirMascota)
                .toList();
        Map<String, Object> resultado = new LinkedHashMap<>();
        resultado.put("mascotas", mascotas);
        resultado.put("total", mascotas.size());
        return resultado;
    }

    private Map<String, Object> buscarCliente(JsonNode argumentos) {
        String texto = argumentos.path("texto").asText("").trim().toLowerCase(Locale.ROOT);
        List<Map<String, Object>> clientes = usuarioRepository.findByRolesNombre("ROLE_CLIENTE").stream()
                .filter(cliente -> coincidePalabras(texto, cliente.getNombres(), cliente.getApellidos(),
                        cliente.getEmail(), cliente.getDocumento()))
                .limit(LIMITE_RESULTADOS)
                .map(this::resumirCliente)
                .toList();
        Map<String, Object> resultado = new LinkedHashMap<>();
        resultado.put("clientes", clientes);
        resultado.put("total", clientes.size());
        return resultado;
    }

    private Map<String, Object> historialMascota(JsonNode argumentos) {
        long mascotaId = argumentos.path("mascotaId").asLong(-1);
        if (mascotaId <= 0) {
            return Map.of("error", "Debes indicar el id de la mascota (parametro mascotaId).");
        }
        Mascota mascota = mascotaRepository.findById(mascotaId).orElse(null);
        if (mascota == null) {
            return Map.of("error", "No existe una mascota con id " + mascotaId);
        }
        if (esClienteSinPermisosInternos() && !esMascotaDelUsuario(mascota)) {
            return Map.of("error", "No se encontró esa mascota en tus registros.");
        }
        List<Map<String, Object>> eventos = mascotaService.historial(mascotaId).stream()
                .limit(LIMITE_RESULTADOS)
                .map(this::resumirHistorial)
                .toList();
        Map<String, Object> resultado = new LinkedHashMap<>();
        resultado.put("mascota", resumirMascota(mascota));
        resultado.put("historial", eventos);
        resultado.put("total", eventos.size());
        return resultado;
    }

    private Map<String, Object> stockFarmacia(JsonNode argumentos) {
        String texto = argumentos.path("texto").asText("").trim().toLowerCase(Locale.ROOT);
        List<Map<String, Object>> productos = productoRepository.findAll().stream()
                .filter(producto -> coincidePalabras(texto, producto.getCodigo(), producto.getNombre(),
                        producto.getCategoria()))
                .limit(LIMITE_RESULTADOS)
                .map(this::resumirProducto)
                .toList();
        Map<String, Object> resultado = new LinkedHashMap<>();
        resultado.put("productos", productos);
        resultado.put("total", productos.size());
        return resultado;
    }

    private Map<String, Object> citasFecha(JsonNode argumentos) {
        String fechaTexto = argumentos.path("fecha").asText("").trim();
        LocalDate fecha;
        try {
            fecha = StringUtils.hasText(fechaTexto) ? LocalDate.parse(fechaTexto) : LocalDate.now();
        } catch (DateTimeParseException ex) {
            return Map.of("error", "La fecha debe tener el formato AAAA-MM-DD.");
        }
        List<Map<String, Object>> citas = citaRepository.findAll().stream()
                .filter(cita -> !esClienteSinPermisosInternos()
                        || (cita.getMascota() != null && esMascotaDelUsuario(cita.getMascota())))
                .filter(cita -> cita.getFechaHora() != null && cita.getFechaHora().toLocalDate().equals(fecha))
                .sorted(Comparator.comparing(Cita::getFechaHora))
                .limit(20)
                .map(this::resumirCita)
                .toList();
        Map<String, Object> resultado = new LinkedHashMap<>();
        resultado.put("fecha", fecha.toString());
        resultado.put("citas", citas);
        resultado.put("total", citas.size());
        return resultado;
    }

    private Map<String, Object> facturasCliente(JsonNode argumentos) {
        long clienteId = argumentos.path("clienteId").asLong(-1);
        String email = argumentos.path("email").asText("").trim();
        Usuario cliente = clienteId > 0
                ? usuarioRepository.findById(clienteId).orElse(null)
                : (StringUtils.hasText(email) ? usuarioRepository.findByEmail(email).orElse(null) : null);
        if (cliente == null) {
            return Map.of("error", "Cliente no encontrado. Indica su email o su id de cliente.");
        }
        List<Map<String, Object>> facturas = facturaRepository.findAll().stream()
                .filter(factura -> factura.getCliente() != null && factura.getCliente().getId().equals(cliente.getId()))
                .sorted(Comparator.comparing(Factura::getFecha, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(LIMITE_FACTURAS)
                .map(this::resumirFactura)
                .toList();
        Map<String, Object> resultado = new LinkedHashMap<>();
        resultado.put("cliente", resumirCliente(cliente));
        resultado.put("facturas", facturas);
        resultado.put("total", facturas.size());
        return resultado;
    }

    private Map<String, Object> cuponesVigentes() {
        List<Map<String, Object>> cupones = cuponService.vigentes().stream()
                .limit(30).map(this::resumirCupon).toList();
        return Map.of("fechaConsulta", LocalDate.now().toString(), "cupones", cupones, "total", cupones.size());
    }

    private Map<String, Object> buscarCupon(JsonNode argumentos) {
        String texto = argumentos.path("texto").asText("").trim().toLowerCase(Locale.ROOT);
        List<Map<String, Object>> cupones = cuponService.listar().stream()
                .filter(cupon -> coincide(texto, cupon.getCodigo(), cupon.getDescripcion(), cupon.getCategoria(),
                        cupon.getTipoDescuento()))
                .limit(LIMITE_RESULTADOS).map(this::resumirCupon).toList();
        return Map.of("cupones", cupones, "total", cupones.size());
    }

    private Map<String, Object> resumirCupon(com.VetSphere.Backend.modules.clinic.domain.Cupon cupon) {
        Map<String, Object> resumen = new LinkedHashMap<>();
        resumen.put("codigo", cupon.getCodigo());
        resumen.put("descripcion", cupon.getDescripcion());
        resumen.put("tipoDescuento", cupon.getTipoDescuento());
        resumen.put("valor", cupon.getValor());
        resumen.put("categoria", cupon.getCategoria());
        resumen.put("producto", cupon.getProducto() == null ? null : cupon.getProducto().getNombre());
        resumen.put("fechaInicio", cupon.getFechaInicio());
        resumen.put("fechaFin", cupon.getFechaFin());
        resumen.put("usosActuales", cupon.getUsosActuales());
        resumen.put("usosMaximos", cupon.getUsosMaximos());
        resumen.put("activo", cupon.getActivo());
        return resumen;
    }

    // ------------------------------------------------------------------
    // Resumenes de entidades (solo los campos que al asistente le sirven)
    // ------------------------------------------------------------------

    private Map<String, Object> resumirMascota(Mascota mascota) {
        Map<String, Object> resumen = new LinkedHashMap<>();
        resumen.put("id", mascota.getId());
        resumen.put("nombre", mascota.getNombre());
        resumen.put("especie", mascota.getEspecie());
        resumen.put("raza", mascota.getRaza());
        resumen.put("estado", mascota.getEstado());
        resumen.put("pesoKg", mascota.getPesoKg());
        if (mascota.getPropietario() != null) {
            resumen.put("propietario", nombreCompleto(mascota.getPropietario()));
            resumen.put("emailPropietario", mascota.getPropietario().getEmail());
        }
        return resumen;
    }

    private Map<String, Object> resumirCliente(Usuario cliente) {
        Map<String, Object> resumen = new LinkedHashMap<>();
        resumen.put("id", cliente.getId());
        resumen.put("nombres", cliente.getNombres());
        resumen.put("apellidos", cliente.getApellidos());
        resumen.put("email", cliente.getEmail());
        resumen.put("documento", cliente.getDocumento());
        resumen.put("telefono", cliente.getTelefono());
        return resumen;
    }

    private Map<String, Object> resumirHistorial(HistorialClinico historial) {
        Map<String, Object> resumen = new LinkedHashMap<>();
        resumen.put("fecha", historial.getFecha());
        resumen.put("tipoEvento", historial.getTipoEvento());
        resumen.put("motivo", historial.getMotivo());
        resumen.put("diagnostico", historial.getDiagnostico());
        resumen.put("tratamiento", historial.getTratamiento());
        resumen.put("temperatura", historial.getTemperatura());
        return resumen;
    }

    private Map<String, Object> resumirProducto(Producto producto) {
        Map<String, Object> resumen = new LinkedHashMap<>();
        resumen.put("codigo", producto.getCodigo());
        resumen.put("nombre", producto.getNombre());
        resumen.put("categoria", producto.getCategoria());
        resumen.put("stock", producto.getStock());
        resumen.put("stockCritico", producto.getStockCritico());
        resumen.put("precio", producto.getPrecio());
        int stock = producto.getStock() == null ? 0 : producto.getStock();
        int stockCritico = producto.getStockCritico() == null ? 0 : producto.getStockCritico();
        resumen.put("enRiesgo", stock <= stockCritico);
        return resumen;
    }

    private Map<String, Object> resumirCita(Cita cita) {
        Map<String, Object> resumen = new LinkedHashMap<>();
        resumen.put("id", cita.getId());
        resumen.put("fechaHora", cita.getFechaHora());
        resumen.put("motivo", cita.getMotivo());
        resumen.put("estado", cita.getEstado());
        if (cita.getMascota() != null) {
            resumen.put("mascota", cita.getMascota().getNombre());
        }
        if (cita.getVeterinario() != null) {
            resumen.put("veterinario", nombreCompleto(cita.getVeterinario()));
        }
        return resumen;
    }

    private Map<String, Object> resumirFactura(Factura factura) {
        Map<String, Object> resumen = new LinkedHashMap<>();
        resumen.put("numero", factura.getNumero());
        resumen.put("fecha", factura.getFecha());
        resumen.put("concepto", factura.getConcepto());
        resumen.put("subtotal", factura.getSubtotal());
        resumen.put("impuestos", factura.getImpuestos());
        resumen.put("total", factura.getTotal());
        resumen.put("estadoPago", factura.getEstadoPago());
        return resumen;
    }

    private static String nombreCompleto(Usuario usuario) {
        return ((usuario.getNombres() == null ? "" : usuario.getNombres()) + " "
                + (usuario.getApellidos() == null ? "" : usuario.getApellidos())).trim();
    }

    /** Coincidencia parcial insensible a mayusculas; texto vacio coincide con todo. */
    private static boolean coincide(String texto, String... campos) {
        if (!StringUtils.hasText(texto)) {
            return true;
        }
        for (String campo : campos) {
            if (campo != null && campo.toLowerCase(Locale.ROOT).contains(texto)) {
                return true;
            }
        }
        return false;
    }

    /** Admite una búsqueda con nombre y apellido aunque estén en columnas separadas. */
    private static boolean coincidePalabras(String texto, String... campos) {
        if (!StringUtils.hasText(texto)) return true;
        String[] terminos = normalizarTexto(texto).split("\\s+");
        String contenido = java.util.Arrays.stream(campos).filter(java.util.Objects::nonNull)
                .map(ChatService::normalizarTexto).collect(java.util.stream.Collectors.joining(" "));
        return java.util.Arrays.stream(terminos).filter(StringUtils::hasText)
                .allMatch(contenido::contains);
    }

    // ------------------------------------------------------------------
    // Definicion de las herramientas expuestas al modelo (estilo OpenAI)
    // ------------------------------------------------------------------

    private static List<Map<String, Object>> definirHerramientas() {
        return List.of(
                herramienta("buscar_mascota",
                        "Busca mascotas registradas por nombre, especie o raza. Devuelve id, dueno y estado.",
                        Map.of("texto", texto("Palabra o fragmento a buscar en nombre, especie o raza")),
                        List.of("texto")),
                herramienta("buscar_cliente",
                        "Busca clientes por nombres, apellidos, correo o documento.",
                        Map.of("texto", texto("Palabra o fragmento a buscar en el cliente")),
                        List.of("texto")),
                herramienta("historial_mascota",
                        "Obtiene los ultimos eventos del historial clinico de una mascota por su id.",
                        Map.of("mascotaId", entero("Identificador numerico de la mascota")),
                        List.of("mascotaId")),
                herramienta("stock_farmacia",
                        "Consulta el inventario de la farmacia por codigo, nombre o categoria. "
                                + "Marca 'enRiesgo' cuando el stock esta en el nivel critico.",
                        Map.of("texto", texto("Codigo, nombre o categoria del producto")),
                        List.of("texto")),
                herramienta("citas_fecha",
                        "Lista las citas programadas para una fecha (AAAA-MM-DD). Si no se indica fecha usa hoy.",
                        Map.of("fecha", texto("Fecha en formato AAAA-MM-DD; vacia = hoy")),
                        List.of()),
                herramienta("facturas_cliente",
                        "Lista las facturas de un cliente identificado por email o por id de cliente.",
                        Map.of("email", texto("Correo electronico del cliente"),
                                "clienteId", entero("Id numerico del cliente")),
                        List.of()),
                herramienta("cupones_vigentes",
                        "Consulta en tiempo real los cupones activos que estan vigentes hoy.",
                        Map.of(), List.of()),
                herramienta("buscar_cupon",
                        "Busca cupones, incluidos inactivos o vencidos, por codigo, descripcion, categoria o tipo.",
                        Map.of("texto", texto("Codigo o texto a buscar; vacio devuelve los cupones recientes")),
                        List.of("texto")));
    }

    private static Map<String, Object> herramienta(String nombre, String descripcion,
                                                   Map<String, Object> propiedades, List<String> requeridos) {
        Map<String, Object> funcion = new LinkedHashMap<>();
        funcion.put("name", nombre);
        funcion.put("description", descripcion);
        Map<String, Object> parametros = new LinkedHashMap<>();
        parametros.put("type", "object");
        parametros.put("properties", propiedades);
        parametros.put("required", requeridos);
        funcion.put("parameters", parametros);
        Map<String, Object> herramienta = new LinkedHashMap<>();
        herramienta.put("type", "function");
        herramienta.put("function", funcion);
        return herramienta;
    }

    private static Map<String, Object> texto(String descripcion) {
        return Map.of("type", "string", "description", descripcion);
    }

    private static Map<String, Object> entero(String descripcion) {
        return Map.of("type", "integer", "description", descripcion);
    }
}
