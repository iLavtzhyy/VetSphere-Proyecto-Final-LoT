package com.VetSphere.Backend.modules.clinic.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Codigo de verificacion de un solo uso enviado por correo electronico.
 * El campo {@code tipo} evita que un codigo emitido para un proposito
 * (por ejemplo, recuperar la contrasena) se pueda reutilizar para otro
 * (por ejemplo, desactivar la cuenta).
 */
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@Entity
public class CodigoVerificacion {
    public static final String VERIFICACION_CUENTA = "VERIFICACION_CUENTA";
    public static final String RECUPERACION = "RECUPERACION";
    public static final String DESACTIVACION = "DESACTIVACION";
    public static final String REACTIVACION = "REACTIVACION";
    public static final String SEGUNDO_FACTOR_CORREO = "SEGUNDO_FACTOR_CORREO";

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String email;
    private String codigo;
    private LocalDateTime expiraEn;
    private boolean usado;
    @Column(nullable = false, length = 30)
    private String tipo = VERIFICACION_CUENTA;
}
