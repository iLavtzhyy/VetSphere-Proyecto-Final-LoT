package com.VetSphere.Backend.shared.response;

import java.util.List;

/**
 * Envoltorio de paginacion opcional.
 * Solo se usa cuando el cliente envia los parametros page/size; en caso contrario
 * los endpoints siguen devolviendo el arreglo JSON de siempre dentro de data.
 */
public record PaginaResponse<T>(List<T> contenido, int pagina, int tamano, long totalElementos, int totalPaginas) {
}
