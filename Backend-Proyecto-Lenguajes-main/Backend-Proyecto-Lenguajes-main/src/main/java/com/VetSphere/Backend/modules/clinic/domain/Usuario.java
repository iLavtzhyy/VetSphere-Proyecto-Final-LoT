package com.VetSphere.Backend.modules.clinic.domain;

import jakarta.persistence.*;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@Entity
public class Usuario {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String nombres;
    @Column(nullable = false)
    private String apellidos;
    @Column(nullable = false, unique = true)
    private String email;
    private String telefono;
    @JsonIgnore
    private String password;
    @JsonIgnore
    @Column(length = 64)
    private String secretoAutenticador;
    @Builder.Default
    private Boolean autenticadorHabilitado = false;
    @Builder.Default
    private Boolean segundoFactorCorreoHabilitado = false;
    @JsonIgnore
    @Column(length = 1200)
    private String codigosRespaldoHash;
    private String fotoUrl;
    private String especialidad;
    private String cedulaProfesional;
    @Column(length = 1200)
    private String biografia;
    private String documento;
    private String ciudad;
    private boolean activo;

    /**
     * Intentos de inicio de sesion fallidos consecutivos. Se usa NULL para las
     * cuentas heredadas (NULL equivale a 0) y evitar problemas de esquema al
     * agregar la columna a bases ya creadas.
     */
    private Integer intentosFallidos;

    /** Instante hasta el que la cuenta permanece bloqueada (NULL = sin bloqueo). */
    private LocalDateTime bloqueadoHasta;

    /** Motivo del bloqueo: manual (administrador) o automatico (intentos fallidos). */
    private String motivoBloqueo;

    /**
     * Marca de verificacion del correo electronico. NULL = cuenta heredada ya
     * verificada; FALSE = debe verificarse antes de operar.
     */
    private Boolean emailVerificado;

    /** Ultimo inicio de sesion exitoso (auditoria y cuentas inactivas). */
    private LocalDateTime ultimoAcceso;

    /** Fecha en que la cuenta fue desactivada por el propio titular o por un admin. */
    private LocalDateTime desactivadoEn;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "usuario_roles", joinColumns = @JoinColumn(name = "usuario_id"), inverseJoinColumns = @JoinColumn(name = "rol_id"))
    @Builder.Default
    private Set<Rol> roles = new HashSet<>();

    /** Numero de intentos fallidos exitosos (NULL se trata como 0). */
    public int intentosFallidosEfectivos() {
        return intentosFallidos == null ? 0 : intentosFallidos;
    }

    /** true si el bloqueo automatico o manual sigue vigente. */
    public boolean bloqueadoAhora() {
        return bloqueadoHasta != null && bloqueadoHasta.isAfter(LocalDateTime.now());
    }

    /** true si el correo esta verificado (las cuentas heredadas se consideran verificadas). */
    public boolean correoVerificado() {
        return emailVerificado == null || emailVerificado;
    }
}
