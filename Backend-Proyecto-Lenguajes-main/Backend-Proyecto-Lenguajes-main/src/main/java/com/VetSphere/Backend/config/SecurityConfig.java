package com.VetSphere.Backend.config;

import com.VetSphere.Backend.security.JwtAccessDeniedHandler;
import com.VetSphere.Backend.security.JwtAuthenticationEntryPoint;
import com.VetSphere.Backend.security.JwtAuthenticationFilter;
import com.VetSphere.Backend.security.ReglasSeguridad;
import com.VetSphere.Backend.security.Roles;
import com.VetSphere.Backend.modules.clinic.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Cadena de seguridad global: stateless, JWT en cabecera Authorization y reglas
 * explicitas por modulo. La normalizacion de roles vive en
 * {@link com.VetSphere.Backend.security.Roles} (un unico punto de verdad).
 *
 * <p>Las reglas por URL no se escriben aqui: se toman de
 * {@link ReglasSeguridad}, la misma tabla de la que se deduce la matriz de
 * permisos que ve la interfaz. Asi ambos puntos de vista no pueden divergir.
 * En Spring Security gana la PRIMERA regla que coincide, por lo que el orden
 * de la tabla importa.</p>
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http.csrf(csrf -> csrf.disable())
                .cors(cors -> {})
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(eh -> eh
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                        // 403 con envoltorio ApiResponse (no cuerpo vacio de sendError)
                        .accessDeniedHandler(jwtAccessDeniedHandler))
                .authorizeHttpRequests(auth -> {
                    for (ReglasSeguridad.Regla regla : ReglasSeguridad.REGLAS) {
                        String[] patrones = regla.patrones().toArray(String[]::new);
                        if (regla.metodos().isEmpty()) {
                            aplicar(auth.requestMatchers(patrones), regla);
                        } else {
                            for (HttpMethod metodo : regla.metodos()) {
                                aplicar(auth.requestMatchers(metodo, patrones), regla);
                            }
                        }
                    }
                    // Lo que no aparece en la tabla solo exige sesion iniciada.
                    auth.anyRequest().authenticated();
                })
                .headers(headers -> headers
                        .frameOptions(frame -> frame.sameOrigin())
                        .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'self'; img-src 'self' data: https:; script-src 'self'; style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; font-src 'self' https://fonts.gstatic.com"))
                        .httpStrictTransportSecurity(hsts -> hsts.includeSubDomains(true).maxAgeInSeconds(31536000)))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    /** Traduce el modo de la regla a la llamada que corresponde de Spring Security. */
    private void aplicar(
            org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer<org.springframework.security.config.annotation.web.builders.HttpSecurity>.AuthorizedUrl matcher,
            ReglasSeguridad.Regla regla) {
        switch (regla.modo()) {
            case PUBLICO -> matcher.permitAll();
            case AUTENTICADO -> matcher.authenticated();
            case ROLES -> matcher.hasAnyRole(regla.roles().toArray(String[]::new));
        }
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService(UsuarioRepository usuarioRepository) {
        return email -> usuarioRepository.findByEmail(email)
                .map(usuario -> User.withUsername(usuario.getEmail())
                        .password(usuario.getPassword())
                        .disabled(!usuario.isActivo())
                        .authorities(usuario.getRoles().stream()
                                .flatMap(rol -> Roles.normalizar(rol.getNombre()))
                                .distinct()
                                .toArray(String[]::new))
                        .build())
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));
    }
}
