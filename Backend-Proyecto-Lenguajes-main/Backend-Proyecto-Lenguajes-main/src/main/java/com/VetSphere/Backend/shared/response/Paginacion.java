package com.VetSphere.Backend.shared.response;

import java.util.List;

/**
 * Utilidad para la paginacion OPCIONAL de los listados.
 * El contrato con el frontend no cambia: solo cuando el cliente envia page/size
 * la data se envuelve en {@link PaginaResponse}; en caso contrario se devuelve
 * el arreglo JSON de siempre.
 */
public final class Paginacion {
    private static final int TAMANO_MAXIMO = 100;
    private static final int TAMANO_DEFECTO = 10;

    private Paginacion() {
    }

    /** Indica si el cliente solicito paginacion. */
    public static boolean solicitada(Integer page, Integer size) {
        return page != null || size != null;
    }

    public static int pagina(Integer page) {
        return page == null ? 0 : Math.max(0, page);
    }

    public static int tamano(Integer size) {
        return size == null ? TAMANO_DEFECTO : Math.min(TAMANO_MAXIMO, Math.max(1, size));
    }

    /** Variante en memoria: pagina una lista ya cargada (utile para listados pequeños). */
    public static <T> ApiResponse<Object> responder(String mensaje, List<T> contenido, Integer page, Integer size) {
        if (!solicitada(page, size)) {
            return responder(mensaje, contenido, contenido == null ? 0 : contenido.size(), null, null);
        }
        List<T> lista = contenido == null ? List.of() : contenido;
        int pagina = pagina(page);
        int tamano = tamano(size);
        long totalElementos = lista.size();
        int desde = Math.min(lista.size(), pagina * tamano);
        int hasta = Math.min(lista.size(), desde + tamano);
        return responder(mensaje, lista.subList(desde, hasta), totalElementos, page, size);
    }

    /** Construye la respuesta paginada o el arreglo simple segun lo solicitado. */
    public static <T> ApiResponse<Object> responder(String mensaje, List<T> contenido, long totalElementos,
                                                    Integer page, Integer size) {
        if (!solicitada(page, size)) {
            return ApiResponse.<Object>ok(mensaje, contenido);
        }
        int pagina = pagina(page);
        int tamano = tamano(size);
        int totalPaginas = totalElementos == 0 ? 0 : (int) Math.ceil((double) totalElementos / tamano);
        return ApiResponse.<Object>ok(mensaje,
                new PaginaResponse<>(contenido, pagina, tamano, totalElementos, totalPaginas));
    }
}
