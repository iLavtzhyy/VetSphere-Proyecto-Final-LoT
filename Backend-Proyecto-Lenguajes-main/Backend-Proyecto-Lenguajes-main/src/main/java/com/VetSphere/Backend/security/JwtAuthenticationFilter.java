package com.VetSphere.Backend.security;

import com.VetSphere.Backend.modules.clinic.repository.UsuarioRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * Autentica las peticiones a partir del JWT enviado en la cabecera Authorization.
 * Un token invalido o expirado NUNCA interrumpe la peticion: simplemente deja el
 * contexto de seguridad vacio y la regla de autorizacion/EntryPoint responde 401.
 *
 * El filtro tambien valida en cada peticion que la cuenta siga activa y NO este
 * bloqueada: de este modo, bloquear o desactivar a un usuario invalida de
 * inmediato sus tokens ya emitidos.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final UsuarioRepository usuarioRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                String email = jwtService.extraerEmail(token);
                usuarioRepository.findByEmail(email)
                        .filter(Usuario -> Usuario.isActivo())
                        .filter(Usuario -> !Usuario.bloqueadoAhora())
                        .ifPresent(usuario -> {
                            var authorities = usuario.getRoles().stream()
                                    .flatMap(rol -> Roles.normalizar(rol.getNombre()))
                                    .distinct()
                                    .map(SimpleGrantedAuthority::new)
                                    .toList();
                            var auth = new UsernamePasswordAuthenticationToken(email, null, authorities);
                            SecurityContextHolder.getContext().setAuthentication(auth);
                        });
            } catch (Exception ex) {
                // Token invalido, expirado o con firma incorrecta: contexto vacio (luego responde 401).
                log.debug("Token JWT descartado en {}: {}", request.getRequestURI(), ex.getMessage());
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }
}
