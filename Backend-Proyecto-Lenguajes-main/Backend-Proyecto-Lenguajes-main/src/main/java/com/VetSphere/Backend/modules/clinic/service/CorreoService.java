package com.VetSphere.Backend.modules.clinic.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.VetSphere.Backend.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;

/**
 * Envio de correos transaccionales (2FA, recuperacion, bloqueo y baja de cuenta).
 *
 * Cuando no hay remitente configurado el envio se SIMULA: se registra en el log
 * para poder completar la demo, pero NUNCA se imprime el codigo en el log cuando
 * el envio real es posible. Asi se evita filtrar credenciales en archivos de log.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CorreoService {
    private final JavaMailSender mailSender;
    @Value("${spring.mail.username:}")
    private String correoRemitente;

    /** true si hay un remitente configurado (envio real). */
    public boolean envioReal() {
        return correoRemitente != null && !correoRemitente.isBlank();
    }

    public void enviarCodigoDosPasos(String email, String codigo) {
        if (!envioReal()) {
            log.info("[CORREO SIMULADO] Codigo de verificacion de cuenta generado para {}. "
                    + "Configure MAIL_USERNAME para enviarlo por correo.", email);
            return;
        }
        enviar(email, "VetSphere - Verifica tu correo",
                "Hola,\n\nTu codigo de verificacion para VetSphere es: " + codigo
                        + "\n\nEl codigo expira en 10 minutos. Si no creaste esta cuenta, ignora este mensaje.");
    }

    public void enviarCodigoSegundoFactor(String email, String codigo) {
        if (!envioReal()) {
            log.info("[CORREO SIMULADO] Código de acceso de segundo factor generado para {}. Configure MAIL_USERNAME para enviarlo por correo.", email);
            return;
        }
        String texto = "Hola,\n\nUsa este código para confirmar tu identidad e iniciar sesión en VetSphere:\n\n"
                + codigo + "\n\nEl código vence en 10 minutos y solo se puede usar una vez. "
                + "Si no intentaste entrar, ignora este correo y cambia tu contraseña.";
        enviarHtml(email, "Tu código de acceso | VetSphere", texto, codigo);
    }

    public void enviarCodigoRecuperacion(String email, String codigo) {
        if (!envioReal()) {
            log.info("[CORREO SIMULADO] Codigo de recuperacion generado para {}. "
                    + "Configure MAIL_USERNAME para enviarlo por correo.", email);
            return;
        }
        try {
            SimpleMailMessage mensaje = new SimpleMailMessage();
            mensaje.setFrom(correoRemitente);
            mensaje.setTo(email);
            mensaje.setSubject("VetSphere | Código para restablecer tu contraseña");
            mensaje.setText("Hola,\n\nRecibimos una solicitud para restablecer la contraseña de tu cuenta VetSphere.\n\n"
                    + "Tu código de verificación es: " + codigo + "\n\n"
                    + "El código vence en 10 minutos y solo puede usarse una vez. Si no solicitaste este cambio, "
                    + "ignora este correo. No compartas el código con nadie.");
            mailSender.send(mensaje);
        } catch (Exception ex) {
            log.warn("No se pudo enviar el correo de recuperación a {}: {}", email, ex.getMessage());
            throw new BusinessException("No se pudo enviar el correo de recuperación. Revisa la configuración de Gmail e inténtalo nuevamente.");
        }
    }

    public void enviarCodigoDesactivacion(String email, String codigo) {
        if (!envioReal()) {
            log.info("[CORREO SIMULADO] Codigo de desactivacion de cuenta generado para {}. "
                    + "Configure MAIL_USERNAME para enviarlo por correo.", email);
            return;
        }
        enviar(email, "VetSphere - Confirmacion de desactivacion",
                "Hola,\n\nRecibimos una solicitud para desactivar tu cuenta de VetSphere.\n"
                        + "Tu codigo de confirmacion es: " + codigo
                        + "\n\nExpira en 10 minutos. Si no fuiste tu, ignora este mensaje: la cuenta seguira activa.");
    }

    public void enviarCodigoReactivacion(String email, String codigo) {
        if (!envioReal()) {
            log.info("[CORREO SIMULADO] Codigo de reactivacion de cuenta generado para {}. "
                    + "Configure MAIL_USERNAME para enviarlo por correo.", email);
            return;
        }
        enviar(email, "VetSphere - Reactivacion de cuenta",
                "Hola,\n\nRecibimos una solicitud para reactivar tu cuenta de VetSphere.\n"
                        + "Tu codigo de confirmacion es: " + codigo
                        + "\n\nExpira en 10 minutos. Si no fuiste tu, ignora este mensaje.");
    }

    /** Aviso al titular de que su cuenta fue bloqueada automaticamente. */
    public void enviarAvisoBloqueo(String email, int horas) {
        if (!envioReal()) {
            log.info("[CORREO SIMULADO] Aviso de bloqueoAutomatico para {} ({} horas).", email, horas);
            return;
        }
        enviar(email, "VetSphere - Cuenta bloqueada temporalmente",
                "Hola,\n\nTu cuenta fue bloqueada por " + horas + " horas debido a intentos fallidos de acceso.\n"
                        + "Puedes volver a entrar tras ese periodo o solicitar la reactivacion por correo.");
    }

    /** Aviso al titular de que un administrador bloqueo o desactivo su cuenta. */
    public void enviarAvisoAdministrativo(String email, String motivo) {
        if (!envioReal()) {
            log.info("[CORREO SIMULADO] Aviso administrativo para {}. Motivo: {}", email, motivo);
            return;
        }
        enviar(email, "VetSphere - Actualizacion de estado de tu cuenta",
                "Hola,\n\nUn administrador de VetSphere actualizo el estado de tu cuenta.\n"
                        + "Motivo: " + (motivo == null || motivo.isBlank() ? "No especificado" : motivo)
                        + "\n\nSi crees que se trata de un error, contacta a la recepcion de la clinica.");
    }

    private void enviar(String destinatario, String asunto, String cuerpo) {
        try {
            SimpleMailMessage mensaje = new SimpleMailMessage();
            mensaje.setFrom(correoRemitente);
            mensaje.setTo(destinatario);
            mensaje.setSubject(asunto);
            mensaje.setText(cuerpo);
            mailSender.send(mensaje);
        } catch (Exception ex) {
            // No se filtra el codigo al log: la recuperacion seguira siendo posible por soporte.
            log.warn("No se pudo enviar el correo '{}' a {}: {}", asunto, destinatario, ex.getMessage());
        }
    }

    /**
     * Mensaje HTML de acceso, con alternativa de texto para clientes de correo
     * que no muestran HTML. El código se mantiene idéntico en ambas versiones.
     */
    private void enviarHtml(String destinatario, String asunto, String texto, String codigo) {
        try {
            MimeMessage mensaje = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, false, "UTF-8");
            helper.setFrom(correoRemitente, "VetSphere | Clínica veterinaria");
            helper.setTo(destinatario);
            helper.setSubject(asunto);
            helper.setText(texto, plantillaCodigoAcceso(codigo));
            mailSender.send(mensaje);
        } catch (Exception ex) {
            log.warn("No se pudo enviar el correo '{}' a {}: {}", asunto, destinatario, ex.getMessage());
        }
    }

    private String plantillaCodigoAcceso(String codigo) {
        String seguro = codigo == null ? "" : codigo.replace("&", "&amp;")
                .replace("<", "&lt;").replace(">", "&gt;");
        return """
                <!doctype html>
                <html lang="es">
                <head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1.0"></head>
                <body style="margin:0;padding:0;background:#eef4f6;font-family:Arial,Helvetica,sans-serif;color:#183542;">
                  <div style="display:none;max-height:0;overflow:hidden;opacity:0;">Tu código de acceso VetSphere vence en 10 minutos.</div>
                  <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" border="0" style="background:#eef4f6;padding:32px 12px;">
                    <tr><td align="center">
                      <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" border="0" style="max-width:560px;background:#ffffff;border:1px solid #dce8eb;border-radius:18px;overflow:hidden;">
                        <tr><td style="background:#0b7181;padding:22px 30px;">
                          <div style="font-size:12px;letter-spacing:2px;font-weight:bold;color:#b8eeea;">CLÍNICA VETERINARIA INTEGRAL</div>
                          <div style="font-size:25px;line-height:1.2;font-weight:bold;color:#ffffff;margin-top:5px;">VetSphere</div>
                        </td></tr>
                        <tr><td style="padding:32px 30px 18px;">
                          <div style="font-size:12px;font-weight:bold;letter-spacing:1.3px;color:#078396;">VERIFICACIÓN DE SEGURIDAD</div>
                          <h1 style="font-size:23px;line-height:1.3;color:#143442;margin:10px 0 12px;">Confirma tu inicio de sesión</h1>
                          <p style="font-size:15px;line-height:1.65;color:#526d78;margin:0;">Ingresa este código en VetSphere para completar el acceso a tu cuenta.</p>
                        </td></tr>
                        <tr><td align="center" style="padding:12px 30px 8px;">
                          <div style="display:inline-block;background:#eff8f8;border:1px solid #cce5e6;border-radius:14px;padding:17px 30px;color:#075e70;font-size:32px;line-height:1.2;font-weight:bold;letter-spacing:8px;">%s</div>
                          <div style="font-size:13px;color:#66808a;margin-top:12px;">Vence en <strong style="color:#294c59;">10 minutos</strong> · válido para un solo uso</div>
                        </td></tr>
                        <tr><td style="padding:24px 30px 30px;">
                          <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" border="0" style="background:#fff8e8;border:1px solid #f2dfb3;border-radius:12px;">
                            <tr><td style="padding:14px 16px;font-size:13px;line-height:1.6;color:#725a2b;"><strong>¿No solicitaste este código?</strong><br>Ignora este mensaje y cambia tu contraseña si sospechas que alguien intentó acceder. VetSphere nunca te pedirá que compartas el código.</td></tr>
                          </table>
                        </td></tr>
                        <tr><td style="border-top:1px solid #e5edef;padding:17px 30px;text-align:center;font-size:11px;line-height:1.5;color:#82969d;">Mensaje automático de seguridad · VetSphere<br>Por favor, no respondas a este correo.</td></tr>
                      </table>
                    </td></tr>
                  </table>
                </body>
                </html>
                """.formatted(seguro);
    }
}
