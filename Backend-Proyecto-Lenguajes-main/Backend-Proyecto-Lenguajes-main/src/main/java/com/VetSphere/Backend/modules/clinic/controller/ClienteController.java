package com.VetSphere.Backend.modules.clinic.controller;

import com.VetSphere.Backend.exception.BusinessException;
import com.VetSphere.Backend.modules.clinic.domain.Usuario;
import com.VetSphere.Backend.modules.clinic.domain.Rol;
import com.VetSphere.Backend.modules.clinic.repository.RolRepository;
import com.VetSphere.Backend.modules.clinic.repository.FacturaRepository;
import com.VetSphere.Backend.modules.clinic.repository.MascotaRepository;
import com.VetSphere.Backend.modules.clinic.repository.UsuarioRepository;
import com.VetSphere.Backend.shared.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;

@RestController
@RequestMapping("/api/clientes")
@RequiredArgsConstructor
public class ClienteController {
    private final UsuarioRepository usuarioRepository;
    private final MascotaRepository mascotaRepository;
    private final FacturaRepository facturaRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;

    @GetMapping
    public ApiResponse<List<Map<String, Object>>> listar() {
        var clientes = usuarioRepository.findByRolesNombre("ROLE_CLIENTE").stream()
                .map(this::mapearCliente)
                .toList();
        return ApiResponse.ok("Clientes", clientes);
    }

    private static final String ABECEDARIO_CLAVE = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";
    private static final int LONGITUD_CLAVE_TEMPORAL = 12;
    private static final SecureRandom ALEATORIO = new SecureRandom();

    @PostMapping
    public ApiResponse<Map<String, Object>> crear(@RequestBody Map<String, String> request) {
        String documento = request.get("documento") == null ? "" : request.get("documento").trim();
        if (!documento.matches("[0-9]{10}")) {
            throw new BusinessException("El número de cédula es obligatorio y debe tener 10 dígitos.");
        }
        var rol = rolRepository.findByNombre("ROLE_CLIENTE").orElseThrow();
        // Nunca se usa una contrasena por defecto hardcodeada: si el cliente no envia una,
        // se genera una aleatoria y se devuelve como "claveTemporal" en data.
        String claveRecibida = request.get("password");
        boolean claveGenerada = claveRecibida == null || claveRecibida.isBlank();
        String clave = claveGenerada ? generarClaveTemporal() : claveRecibida;
        Usuario cliente = Usuario.builder()
                .nombres(request.get("nombres"))
                .apellidos(request.get("apellidos"))
                .email(request.get("email"))
                .telefono(request.get("telefono"))
                .ciudad(request.get("ciudad"))
                .documento(documento)
                .password(passwordEncoder.encode(clave))
                .activo(true)
                // Coleccion mutable: Hibernate no puede vaciar Set.of(...) al hacer merge
                .roles(new HashSet<>(Set.of(rol)))
                .build();
        Usuario guardado = usuarioRepository.save(cliente);
        Map<String, Object> respuesta = mapearCliente(guardado);
        if (claveGenerada) {
            respuesta.put("claveTemporal", clave);
        }
        return ApiResponse.ok("Cliente creado exitosamente", respuesta);
    }

    private String generarClaveTemporal() {
        StringBuilder clave = new StringBuilder(LONGITUD_CLAVE_TEMPORAL);
        for (int i = 0; i < LONGITUD_CLAVE_TEMPORAL; i++) {
            clave.append(ABECEDARIO_CLAVE.charAt(ALEATORIO.nextInt(ABECEDARIO_CLAVE.length())));
        }
        return clave.toString();
    }

    @PutMapping("/{id}")
    public ApiResponse<Map<String, Object>> actualizar(@PathVariable Long id, @RequestBody Map<String, String> request) {
        Usuario cliente = usuarioRepository.findById(id).orElseThrow();
        if (request.containsKey("nombres")) cliente.setNombres(request.get("nombres"));
        if (request.containsKey("apellidos")) cliente.setApellidos(request.get("apellidos"));
        if (request.containsKey("email")) cliente.setEmail(request.get("email"));
        if (request.containsKey("telefono")) cliente.setTelefono(request.get("telefono"));
        if (request.containsKey("ciudad")) cliente.setCiudad(request.get("ciudad"));
        if (request.containsKey("documento")) cliente.setDocumento(request.get("documento"));
        if (request.containsKey("estado")) cliente.setActivo("Activo".equalsIgnoreCase(request.get("estado")));
        Usuario guardado = usuarioRepository.save(cliente);
        return ApiResponse.ok("Cliente actualizado exitosamente", mapearCliente(guardado));
    }

    private Map<String, Object> mapearCliente(Usuario cliente) {
        var mascotas = mascotaRepository.findByPropietarioId(cliente.getId());
        BigDecimal totalGastado = facturaRepository.findAll().stream()
                .filter(factura -> factura.getCliente() != null && factura.getCliente().getId().equals(cliente.getId()))
                .map(factura -> factura.getTotal() == null ? BigDecimal.ZERO : factura.getTotal())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        Map<String, Object> map = new HashMap<>();
        map.put("id", cliente.getId());
        map.put("nombres", cliente.getNombres());
        map.put("apellidos", cliente.getApellidos());
        map.put("email", cliente.getEmail());
        map.put("telefono", cliente.getTelefono());
        map.put("ciudad", cliente.getCiudad() != null ? cliente.getCiudad() : "Quito");
        map.put("documento", cliente.getDocumento() != null ? cliente.getDocumento() : "");
        map.put("estado", cliente.isActivo() ? "Activo" : "Pendiente");
        map.put("fechaRegistro", "2026-05-24");
        map.put("totalGastado", totalGastado);
        map.put("mascotas", mascotas);
        return map;
    }
}

